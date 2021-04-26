package com.dwolla.rsocket.consul;

import com.dwolla.rsocket.Address;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HealthPoller {
  private final int StartingIndex = 0;

  private final String healthUrl = "%s/v1/health/service/%s?passing=true&index=%d&wait=1m";
  private final Gson gson = new Gson();
  private final HttpClient client;
  private final String consulHost;

  protected Set<Address> lastResponse;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public HealthPoller(HttpClient client, String consulHost) {
    this.client = client;
    this.consulHost = consulHost;
  }

  public Flux<Set<Address>> start(String serviceName) {
    return Flux.create(emitter -> {
      Recursive<Function<Integer, CompletableFuture<Integer>>> r = new Recursive<>();
      r.func =
              index ->
                      client
                              .get(String.format(healthUrl, consulHost, serviceName, index))
                              .thenApply(
                                      res -> {
                                        int nextIdx = getNextIdxFrom(res);

                                        if (nextIdx > index) {
                                          lastResponse = getAddressesFrom(res.getBody());
                                          emitter.next(lastResponse);
                                        }

                                        return nextIdx;
                                      })
                              .whenComplete(
                                      (nextId, th) -> {
                                        if (th != null) {
                                          logger.warn("Got an exception while long polling Consul.", th);
                                          Mono.delay(Duration.ofSeconds(5))
                                                  .subscribe(i -> r.func.apply(StartingIndex));
                                        } else {
                                          r.func.apply(nextId);
                                        }
                                      });

      r.func.apply(StartingIndex);
    });
  }

  private Integer getNextIdxFrom(SimpleResponse response) {
    return response.getHeaders().stream()
        .filter(e -> e.getKey().equals("X-Consul-Index"))
        .findFirst()
        .map(Map.Entry::getValue)
        .map(Integer::parseInt)
        .orElse(StartingIndex);
  }

  private Set<Address> getAddressesFrom(String body) {
    return Arrays.stream(gson.fromJson(body, HealthDto[].class))
        .map(hr -> new Address(hr.getService().getAddress(), hr.getService().getPort()))
        .collect(Collectors.toCollection(HashSet::new));
  }

  private static class Recursive<I> {
    I func;
  }
}
