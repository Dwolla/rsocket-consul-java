package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.HealthPoller;
import io.rsocket.RSocketFactory;
import io.rsocket.client.LoadBalancedRSocketMono;
import io.rsocket.client.filter.RSocketSupplier;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConsulRSocketLoadBalancerFactory {
  private HealthPoller poller;

  private Logger logger = LoggerFactory.getLogger(getClass());

  protected Function<Address, RSocketSupplier> rsocketBuilder =
      add ->
          new RSocketSupplier(
              () ->
                  RSocketFactory.connect()
                      .transport(TcpClientTransport.create(add.getHost(), add.getPort()))
                      .start());

  public ConsulRSocketLoadBalancerFactory(HealthPoller poller) {
    this.poller = poller;
  }

  public LoadBalancedRSocketMono create(String service) {
    logger.debug("Creating a LoadBalancedRSocketMono for service={}", service);

    poller.start(service);

    return LoadBalancedRSocketMono.create(
        Flux.<Set<Address>>create(
                c -> poller.setListener(c::next), FluxSink.OverflowStrategy.LATEST)
            .map(a -> a.stream().map(rsocketBuilder).collect(Collectors.toSet())));
  }
}
