package com.dwolla.rsocket;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.client.filter.RSocketSupplier;
import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsulRSocketLoadBalancerShould {

  private HealthStream healthStream;
  private ConsulRSocketLoadBalancerFactory loadBalancerFactory;
  private Address expected = new Address("host1", 1234);
  private RSocketSupplier supplier = new RSocketSupplier(() -> Mono.just(new TestRSocket()));

  private Function<Address, RSocketSupplier> builder =
      add -> {
        if (add.equals(expected)) {
          return supplier;
        } else {
          return null;
        }
      };

  @BeforeEach
  void setup() {
    healthStream = mock(HealthStream.class);
    loadBalancerFactory = new ConsulRSocketLoadBalancerFactoryTest(healthStream, builder);
  }

  @Test
  void buildSuppliersFromAddresses() {
    String SERVICE_NAME = "foo";
    Set<Address> addresses = new HashSet<>();
    addresses.add(expected);

    when(healthStream.getHealthyInstancesOf(SERVICE_NAME)).thenReturn(Flux.just(addresses));

    Mono<Payload> response =
        loadBalancerFactory
            .create(SERVICE_NAME)
            .block()
            .requestResponse(DefaultPayload.create(DefaultPayload.EMPTY_BUFFER));

    assertEquals("success", response.block().getDataUtf8());
  }
}

class ConsulRSocketLoadBalancerFactoryTest extends ConsulRSocketLoadBalancerFactory {
  ConsulRSocketLoadBalancerFactoryTest(
          HealthStream healthStream, Function<Address, RSocketSupplier> rsocketBuilder) {
    super(healthStream);
    this.rsocketBuilder = rsocketBuilder;
  }
}

class TestRSocket extends AbstractRSocket {
  @Override
  public Mono<Payload> requestResponse(final Payload payload) {
    return Mono.just(DefaultPayload.create("success"));
  }
}
