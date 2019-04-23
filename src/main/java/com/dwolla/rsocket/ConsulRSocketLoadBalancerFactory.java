package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.HealthPoller;
import io.rsocket.client.LoadBalancedRSocketMono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Set;
import java.util.stream.Collectors;

public class ConsulRSocketLoadBalancerFactory {
  private Logger logger = LoggerFactory.getLogger(getClass());

  private HealthPoller poller;
  private AddressRSocketSupplierFactory supplierFactory;

  public ConsulRSocketLoadBalancerFactory(
      HealthPoller poller, AddressRSocketSupplierFactory supplierFactory) {
    this.poller = poller;
    this.supplierFactory = supplierFactory;
  }

  public LoadBalancedRSocketMono create(String service) {
    logger.debug("Creating a LoadBalancedRSocketMono for service={}", service);

    poller.start(service);

    return LoadBalancedRSocketMono.create(
        Flux.<Set<Address>>create(
                c -> poller.setListener(c::next), FluxSink.OverflowStrategy.LATEST)
            .map(a -> a.stream().map(supplierFactory::create).collect(Collectors.toSet())));
  }
}
