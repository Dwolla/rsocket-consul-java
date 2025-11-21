package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.AsyncHttpClientImpl;
import com.dwolla.rsocket.consul.HealthPoller;
import com.dwolla.rsocket.consul.HttpClient;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;
import java.io.IOException;

public class LoadBalancerBuilder {
  private String consulHost = "http://localhost:8500";
  private static final Duration requestTimeout = Duration.ofSeconds(90);

  private final HttpClient httpClient;

  public LoadBalancerBuilder(final HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public LoadBalancerBuilder() {
    this(new AsyncHttpClientImpl(asyncHttpClient(config().setRequestTimeout(requestTimeout).setReadTimeout(requestTimeout).build())));
  }

  private static List<LoadbalanceTarget> addressesToLoadbalanceTargets(Set<Address> addresses) {
    return addresses.stream()
            .map(address ->
                    LoadbalanceTarget.from(address.toString(), TcpClientTransport.create(address.getHost(), address.getPort())))
            .collect(Collectors.toList());
  }

  public LoadBalancerBuilder withConsul(final String consul) {
    this.consulHost = consul;
    return this;
  }

  public LoadbalanceRSocketClient build(final String service) {
    final Callable<HealthPoller> healthPollerSupplier = () -> new HealthPoller(httpClient, consulHost);

    final Function<HealthPoller, Publisher<? extends List<LoadbalanceTarget>>> healthPollerPublisherFunction =
            poller -> poller.start(service)
                    .map(LoadBalancerBuilder::addressesToLoadbalanceTargets);

    final Consumer<HealthPoller> healthPollerCleanup = poller -> {
      try {
        poller.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };

    final Flux<List<LoadbalanceTarget>> targets =
            Flux.using(healthPollerSupplier, healthPollerPublisherFunction, healthPollerCleanup);

    return LoadbalanceRSocketClient.builder(targets).build();
  }
}
