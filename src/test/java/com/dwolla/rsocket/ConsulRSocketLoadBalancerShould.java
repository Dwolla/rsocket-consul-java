package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.HealthPoller;
import com.dwolla.rsocket.consul.HttpClient;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.client.filter.RSocketSupplier;
import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsulRSocketLoadBalancerShould {
  private Address ADDRESS = new Address("host1", 1234);
  private Set<Address> ADDRESSES = new HashSet<>(Collections.singletonList(ADDRESS));

  private FakePoller poller;
  private AddressRSocketSupplierFactory supplierFactory;
  private ConsulRSocketLoadBalancerFactory loadBalancerFactory;
  private RSocketSupplier supplier = new RSocketSupplier(() -> Mono.just(new TestRSocket()));

  @BeforeEach
  void setup() {
    poller = new FakePoller(null, null, ADDRESSES);
    supplierFactory = mock(AddressRSocketSupplierFactory.class);
    loadBalancerFactory = new ConsulRSocketLoadBalancerFactory(poller, supplierFactory);
  }

  @Test
  void buildSuppliersFromAddresses() {
    String serviceName = "foo";

    when(supplierFactory.create(ADDRESS)).thenReturn(supplier);

    Mono<Payload> response =
        loadBalancerFactory
            .create(serviceName)
            .block()
            .requestResponse(DefaultPayload.create(DefaultPayload.EMPTY_BUFFER));

    assertEquals(serviceName, poller.serviceName);
    assertEquals("success", response.block().getDataUtf8());
  }
}

class TestRSocket extends AbstractRSocket {
  @Override
  public Mono<Payload> requestResponse(final Payload payload) {
    return Mono.just(DefaultPayload.create("success"));
  }
}

class FakePoller extends HealthPoller {
  String serviceName = "";

  FakePoller(HttpClient client, String consulHost, Set<Address> addresses) {
    super(client, consulHost);
    this.lastResponse = addresses;
  }

  @Override
  public void start(String serviceName) {
    this.serviceName = serviceName;
  }
}
