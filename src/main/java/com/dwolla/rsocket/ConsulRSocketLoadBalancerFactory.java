package com.dwolla.rsocket;

import io.rsocket.RSocketFactory;
import io.rsocket.client.LoadBalancedRSocketMono;
import io.rsocket.client.filter.RSocketSupplier;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.stream.Collectors;

public class ConsulRSocketLoadBalancerFactory {
  private HealthStream healthStream;

  private Logger logger = LoggerFactory.getLogger(getClass());

  protected Function<Address, RSocketSupplier> rsocketBuilder =
      add ->
          new RSocketSupplier(
              () ->
                  RSocketFactory.connect()
                      .transport(TcpClientTransport.create(add.getHost(), add.getPort()))
                      .start());

  public ConsulRSocketLoadBalancerFactory(HealthStream healthStream) {
    this.healthStream = healthStream;
  }

  public LoadBalancedRSocketMono create(String service) {
    logger.debug("Creating a LoadBalancedRSocketMono for service={}", service);

    return LoadBalancedRSocketMono.create(
        healthStream
            .getHealthyInstancesOf(service)
            .map(a -> a.stream().map(rsocketBuilder).collect(Collectors.toSet())));
  }
}
