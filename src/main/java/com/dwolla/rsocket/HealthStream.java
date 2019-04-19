package com.dwolla.rsocket;

import reactor.core.publisher.Flux;

import java.util.Set;

public interface HealthStream {
  Flux<Set<Address>> getHealthyInstancesOf(String service);
}
