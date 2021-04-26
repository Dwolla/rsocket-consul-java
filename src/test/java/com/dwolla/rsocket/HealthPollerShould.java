package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.HealthPoller;
import com.dwolla.rsocket.consul.HttpClient;
import com.dwolla.rsocket.consul.SimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthPollerShould {
  private final String CONSUL_HOST = "https://consul-host.location";
  private final String SERVICE_NAME = "some-name";
  private final Address host1 = new Address("host1", 1234);
  private final Address host2 = new Address("host2", 5678);
  private HealthPoller poller;
  private HttpClient client;
  private final String firstIndex = "34234";
  private Map<String, String> firstHeaders = Collections.singletonMap("X-Consul-Index", firstIndex);

  @BeforeEach
  void setup() {
    client = mock(HttpClient.class);
    poller = new HealthPoller(client, CONSUL_HOST);
  }

  private final String INSTANCE_1 = "{`Service`:{`Address`:`host1`,`Port`:1234}}".replace('`', '"');
  private final String INSTANCE_2 = "{`Service`:{`Address`:`host2`,`Port`:5678}}".replace('`', '"');
  private final String JSON_RES = ("[" + INSTANCE_1 + "," + INSTANCE_2 + "]");

  @Test
  void createAFluxThatIsFedByUpdatesFromConsul() throws ExecutionException, InterruptedException {
    final SimpleResponse res = new SimpleResponse(JSON_RES, new ArrayList<>(firstHeaders.entrySet()));
    final CompletableFuture<SimpleResponse> future = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(future);

    final Flux<Set<Address>> addresses = poller.start(SERVICE_NAME);

    future.complete(res);

    final Set<Address> output = addresses.blockFirst(Duration.ofSeconds(2));

    assertNotNull(output);
    assertTrue(output.contains(host1));
    assertTrue(output.contains(host2));
  }

  @Test
  void notifyTheListenerOfTheMostRecentResponseIfAlreadyResolved() throws ExecutionException, InterruptedException {
    final SimpleResponse res = new SimpleResponse(JSON_RES, new ArrayList<>(firstHeaders.entrySet()));
    final CompletableFuture<SimpleResponse> future = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(future);

    final Flux<Set<Address>> addresses = poller.start(SERVICE_NAME);
    future.complete(res);

    final Set<Address> output = addresses.blockFirst(Duration.ofSeconds(2));

    assertNotNull(output);
    assertTrue(output.contains(host1));
    assertTrue(output.contains(host2));
  }

  @Test
  void continueInitiatingRequests() {
    final Map<String, String> secondHeaders = Collections.singletonMap("X-Consul-Index", "98765");
    final SimpleResponse firstResponse =
        new SimpleResponse("[" + INSTANCE_1 + "]", new ArrayList<>(firstHeaders.entrySet()));
    final SimpleResponse secondResponse =
        new SimpleResponse("[" + INSTANCE_2 + "]", new ArrayList<>(secondHeaders.entrySet()));

    final CompletableFuture<SimpleResponse> firstFuture = new CompletableFuture<>();
    final CompletableFuture<SimpleResponse> secondFuture = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(firstFuture);
    when(client.get(
            CONSUL_HOST
                + "/v1/health/service/"
                + SERVICE_NAME
                + "?passing=true&index="
                + firstIndex
                + "&wait=1m"))
        .thenReturn(secondFuture);

    final Flux<Set<Address>> addresses = poller.start(SERVICE_NAME);

    firstFuture.complete(firstResponse);
    secondFuture.complete(secondResponse);

    final List<Set<Address>> output = addresses.take(2).collect(Collectors.toList()).block();

    assertNotNull(output);
    assertEquals(2, output.size());
    assertTrue(output.get(0).contains(host1));
    assertTrue(output.get(1).contains(host2));
  }

  @Test
  void notNotifyIfTheIndexIsLowerThanThePreviousIndex() {
    final Map<String, String> secondHeaders = Collections.singletonMap("X-Consul-Index", "34233");
    final SimpleResponse firstResponse =
        new SimpleResponse("[" + INSTANCE_1 + "]", new ArrayList<>(firstHeaders.entrySet()));
    final SimpleResponse secondResponse =
        new SimpleResponse("[" + INSTANCE_2 + "]", new ArrayList<>(secondHeaders.entrySet()));

    final CompletableFuture<SimpleResponse> firstFuture = new CompletableFuture<>();
    final CompletableFuture<SimpleResponse> secondFuture = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(firstFuture);
    when(client.get(
            CONSUL_HOST
                + "/v1/health/service/"
                + SERVICE_NAME
                + "?passing=true&index="
                + firstIndex
                + "&wait=1m"))
        .thenReturn(secondFuture);

    final Flux<Set<Address>> addresses = poller.start(SERVICE_NAME);

    firstFuture.complete(firstResponse);
    secondFuture.complete(secondResponse);

    final List<Set<Address>> output = addresses.take(Duration.ofSeconds(1)).collect(Collectors.toList()).block();

    assertNotNull(output);
    assertEquals(1, output.size());
    assertTrue(output.get(0).contains(host1));
  }

  @Test
  void startOverAtIndexZeroAfterFiveSecondsIfAnExceptionOccurs() throws ExecutionException, InterruptedException {
    final SimpleResponse simpleResponse =
        new SimpleResponse(JSON_RES, new ArrayList<>(firstHeaders.entrySet()));
    final CompletableFuture<SimpleResponse> future = new CompletableFuture<>();
    final CompletableFuture<SimpleResponse> failedFuture = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(failedFuture)
        .thenReturn(future);

    final Flux<Set<Address>> addresses = poller.start(SERVICE_NAME);

    failedFuture.completeExceptionally(new RuntimeException("Something happened"));
    future.complete(simpleResponse);

    final Set<Address> output =
            addresses.take(Duration.ofSeconds(10)).next().block(Duration.ofSeconds(10));

    assertNotNull(output);
    assertTrue(output.contains(host1));
    assertTrue(output.contains(host2));
  }
}
