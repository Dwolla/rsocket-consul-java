package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.HealthPoller;
import com.dwolla.rsocket.consul.HttpClient;
import com.dwolla.rsocket.consul.SimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
    SimpleResponse res = new SimpleResponse(JSON_RES, new ArrayList<>(firstHeaders.entrySet()));
    CompletableFuture<SimpleResponse> future = new CompletableFuture<>();
    CompletableFuture<Set<Address>> result = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(future);

    poller.setListener(result::complete);
    poller.start(SERVICE_NAME);

    future.complete(res);

    Set<Address> addresses = result.get();

    assertNotNull(addresses);
    assertTrue(addresses.contains(host1));
    assertTrue(addresses.contains(host2));
  }

  @Test
  void notifyTheListenerOfTheMostRecentResponseIfAlreadyResolved() throws ExecutionException, InterruptedException {
    SimpleResponse res = new SimpleResponse(JSON_RES, new ArrayList<>(firstHeaders.entrySet()));
    CompletableFuture<SimpleResponse> future = new CompletableFuture<>();
    CompletableFuture<Set<Address>> result = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(future);

    poller.start(SERVICE_NAME);
    future.complete(res);
    poller.setListener(result::complete);

    Set<Address> addresses = result.get();

    assertNotNull(addresses);
    assertTrue(addresses.contains(host1));
    assertTrue(addresses.contains(host2));
  }

  @Test
  void continueInitiatingRequests() {
    Map<String, String> secondHeaders = Collections.singletonMap("X-Consul-Index", "98765");
    SimpleResponse firstResponse =
        new SimpleResponse("[" + INSTANCE_1 + "]", new ArrayList<>(firstHeaders.entrySet()));
    SimpleResponse secondResponse =
        new SimpleResponse("[" + INSTANCE_2 + "]", new ArrayList<>(secondHeaders.entrySet()));

    CompletableFuture<SimpleResponse> firstFuture = new CompletableFuture<>();
    CompletableFuture<SimpleResponse> secondFuture = new CompletableFuture<>();
    List<Set<Address>> results = new ArrayList<>();

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

    poller.setListener(results::add);
    poller.start(SERVICE_NAME);

    firstFuture.complete(firstResponse);
    secondFuture.complete(secondResponse);

    assertEquals(2, results.size());
    assertTrue(results.get(0).contains(host1));
    assertTrue(results.get(1).contains(host2));
  }

  @Test
  void notNotifyIfTheIndexIsLowerThanThePreviousIndex() {
    Map<String, String> secondHeaders = Collections.singletonMap("X-Consul-Index", "34233");
    SimpleResponse firstResponse =
        new SimpleResponse("[" + INSTANCE_1 + "]", new ArrayList<>(firstHeaders.entrySet()));
    SimpleResponse secondResponse =
        new SimpleResponse("[" + INSTANCE_2 + "]", new ArrayList<>(secondHeaders.entrySet()));

    CompletableFuture<SimpleResponse> firstFuture = new CompletableFuture<>();
    CompletableFuture<SimpleResponse> secondFuture = new CompletableFuture<>();
    List<Set<Address>> results = new ArrayList<>();

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

    poller.start(SERVICE_NAME);
    poller.setListener(results::add);
    firstFuture.complete(firstResponse);
    secondFuture.complete(secondResponse);

    assertEquals(1, results.size());
    assertTrue(results.get(0).contains(host1));
  }

  @Test
  void startOverAtIndexZeroAfterFiveSecondsIfAnExceptionOccurs()
      throws ExecutionException, InterruptedException {
    SimpleResponse simpleResponse =
        new SimpleResponse(JSON_RES, new ArrayList<>(firstHeaders.entrySet()));
    CompletableFuture<SimpleResponse> future = new CompletableFuture<>();
    CompletableFuture<SimpleResponse> failedFuture = new CompletableFuture<>();
    CompletableFuture<Set<Address>> result = new CompletableFuture<>();

    when(client.get(
            CONSUL_HOST + "/v1/health/service/" + SERVICE_NAME + "?passing=true&index=0&wait=1m"))
        .thenReturn(failedFuture)
        .thenReturn(future);

    poller.start(SERVICE_NAME);
    poller.setListener(result::complete);

    failedFuture.completeExceptionally(new RuntimeException("Something happened"));
    future.complete(simpleResponse);

    Set<Address> addresses = result.get();

    assertNotNull(addresses);
    assertTrue(addresses.contains(host1));
    assertTrue(addresses.contains(host2));
  }
}
