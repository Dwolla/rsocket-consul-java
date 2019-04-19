package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.HttpClient;
import com.dwolla.rsocket.consul.SimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsulHealthStreamShould {
  private final String CONSUL_HOST = "https://consul-host.location";
  private final String SERVICE_NAME = "some-name";
  private final Address host1 = new Address("host1", 1234);
  private final Address host2 = new Address("host2", 5678);
  private ConsulHealthStream healthStream;
  private HttpClient client;

  @BeforeEach
  void setup() {
    client = mock(HttpClient.class);
    healthStream = new ConsulHealthStream(client, CONSUL_HOST);
  }

  private final String INSTANCE_1 = "{`Service`:{`Address`:`host1`,`Port`:1234}}".replace('`', '"');
  private final String INSTANCE_2 = "{`Service`:{`Address`:`host2`,`Port`:5678}}".replace('`', '"');
  private final String JSON_RES = ("[" + INSTANCE_1 + "," + INSTANCE_2 + "]");

  @Test
  void createAFluxThatIsFedByUpdatesFromConsul() {
    Map<String, String> headers = Collections.singletonMap("X-Consul-Index", "34234");
    SimpleResponse simpleResponse =
        new SimpleResponse(JSON_RES, new ArrayList<>(headers.entrySet()));
    CompletableFuture<SimpleResponse> future = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(future);

    Flux<Set<Address>> service = healthStream.getHealthyInstancesOf(SERVICE_NAME);
    future.complete(simpleResponse);

    Set<Address> addresses = service.blockFirst();

    assertNotNull(addresses);
    assertTrue(addresses.contains(host1));
    assertTrue(addresses.contains(host2));
  }

  @Test
  void continueInitiatingRequests() {
    String nextIndex = "34234";
    Map<String, String> firstHeaders = Collections.singletonMap("X-Consul-Index", nextIndex);
    Map<String, String> secondHeaders = Collections.singletonMap("X-Consul-Index", "98765");
    SimpleResponse firstResponse =
        new SimpleResponse("[" + INSTANCE_1 + "]", new ArrayList<>(firstHeaders.entrySet()));
    SimpleResponse secondResponse =
        new SimpleResponse("[" + INSTANCE_2 + "]", new ArrayList<>(secondHeaders.entrySet()));

    CompletableFuture<SimpleResponse> firstFuture = new CompletableFuture<>();
    CompletableFuture<SimpleResponse> secondFuture = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(firstFuture);
    when(client.get(
            CONSUL_HOST
                + "/v1/health/service/"
                + SERVICE_NAME
                + "?passing=true&index="
                + nextIndex
                + "&wait=1m"))
        .thenReturn(secondFuture);

    Flux<Set<Address>> service = healthStream.getHealthyInstancesOf(SERVICE_NAME);
    firstFuture.complete(firstResponse);
    secondFuture.complete(secondResponse);

    List<Set<Address>> sets = service.buffer(2).blockFirst();

    assertNotNull(sets);
    assertTrue(sets.get(0).contains(host1));
    assertTrue(sets.get(1).contains(host2));
  }

  @Test
  void notNotifyIfTheIndexIsLowerThanThePreviousIndex() {
    String nextIndex = "34234";
    Map<String, String> firstHeaders = Collections.singletonMap("X-Consul-Index", nextIndex);
    Map<String, String> secondHeaders = Collections.singletonMap("X-Consul-Index", "34233");
    SimpleResponse firstResponse =
        new SimpleResponse("[" + INSTANCE_1 + "]", new ArrayList<>(firstHeaders.entrySet()));
    SimpleResponse secondResponse =
        new SimpleResponse("[" + INSTANCE_2 + "]", new ArrayList<>(secondHeaders.entrySet()));

    CompletableFuture<SimpleResponse> firstFuture = new CompletableFuture<>();
    CompletableFuture<SimpleResponse> secondFuture = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(firstFuture);
    when(client.get(
            CONSUL_HOST
                + "/v1/health/service/"
                + SERVICE_NAME
                + "?passing=true&index="
                + nextIndex
                + "&wait=1m"))
        .thenReturn(secondFuture);

    Flux<Set<Address>> service = healthStream.getHealthyInstancesOf(SERVICE_NAME);
    firstFuture.complete(firstResponse);
    secondFuture.complete(secondResponse);

    List<Set<Address>> sets = service.bufferTimeout(2, Duration.ofSeconds(1)).blockFirst();

    assertNotNull(sets);
    assertEquals(1, sets.size());
    assertTrue(sets.get(0).contains(host1));
  }

  @Test
  void startOverAtIndexZeroAfterFiveSecondsIfAnExceptionOccurs() {
    Map<String, String> headers = Collections.singletonMap("X-Consul-Index", "34234");
    SimpleResponse simpleResponse =
            new SimpleResponse(JSON_RES, new ArrayList<>(headers.entrySet()));
    CompletableFuture<SimpleResponse> future = new CompletableFuture<>();
    CompletableFuture<SimpleResponse> failedFuture = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
            .thenReturn(failedFuture)
            .thenReturn(future);

    Flux<Set<Address>> service = healthStream.getHealthyInstancesOf(SERVICE_NAME);

    failedFuture.completeExceptionally(new RuntimeException("Something happened"));
    future.complete(simpleResponse);

    Set<Address> addresses = service.blockFirst();

    assertNotNull(addresses);
    assertTrue(addresses.contains(host1));
    assertTrue(addresses.contains(host2));
  }
}
