package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.HealthDto;
import com.dwolla.rsocket.consul.HttpClient;
import com.dwolla.rsocket.consul.SimpleResponse;
import com.google.gson.Gson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConsulHealthStream implements HealthStream {
  private final Gson gson = new Gson();
  private final HttpClient client;
  private String consulHost;

  public ConsulHealthStream(final HttpClient client, final String consulHost) {
    this.client = client;
    this.consulHost = consulHost;
  }

  @Override
  public Flux<Set<Address>> getHealthyInstancesOf(final String service) {
    return Flux.create(c -> startLoop(c::next, service), FluxSink.OverflowStrategy.LATEST);
  }

  private void startLoop(final Consumer<Set<Address>> callback, final String serviceName) {
    Recursive<Function<Integer, CompletableFuture<Integer>>> r = new Recursive<>();
    r.func =
        index ->
            client
                .get(
                    String.format(
                        "%s/v1/health/service/%s?passing=true&index=%d&wait=1m",
                        consulHost, serviceName, index))
                .thenApply(
                    res -> {
                      int nextIdx = getNextIdxFrom(res);

                      if (nextIdx > index) callback.accept(getAddressesFrom(res.getBody()));

                      return nextIdx;
                    })
                .whenComplete(
                    (nextId, th) -> {
                      if (th != null) {
                        Mono.delay(Duration.ofSeconds(5)).subscribe(i -> r.func.apply(0));
                      } else {
                        r.func.apply(nextId);
                      }
                    });

    r.func.apply(0);
  }

  private Integer getNextIdxFrom(SimpleResponse response) {
    return response.getHeaders().stream()
        .filter(e -> e.getKey().equals("X-Consul-Index"))
        .findFirst()
        .map(Map.Entry::getValue)
        .map(Integer::parseInt)
        .orElse(0);
  }

  private Set<Address> getAddressesFrom(String body) {
    return Arrays.stream(gson.fromJson(body, HealthDto[].class))
        .map(hr -> new Address(hr.getService().getAddress(), hr.getService().getPort()))
        .collect(Collectors.toCollection(HashSet::new));
  }
}

class Recursive<I> {
  I func;
}
