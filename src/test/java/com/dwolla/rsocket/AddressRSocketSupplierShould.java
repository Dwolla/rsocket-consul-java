package com.dwolla.rsocket;

import io.rsocket.RSocketFactory;
import io.rsocket.client.filter.RSocketSupplier;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AddressRSocketSupplierShould {
  @Test
  void beEqualToOneAnotherAndHaveTheSameHashCodesIfAddressesMatch() {
    RSocketSupplier sup1 = createFrom(new Address("host1", 1234));
    RSocketSupplier sup2 = createFrom(new Address("host1", 1234));
    RSocketSupplier sup3 = createFrom(new Address("host1", 5678));

    assertEquals(sup1, sup2);
    assertEquals(sup1.hashCode(), sup2.hashCode());

    assertNotEquals(sup2, sup3);
    assertNotEquals(sup2.hashCode(), sup3.hashCode());
  }

  private RSocketSupplier createFrom(Address add) {
    return new AddressRSocketSupplier(
        () ->
            RSocketFactory.connect()
                .transport(TcpClientTransport.create(add.getHost(), add.getPort()))
                .start(),
        add) {};
  }
}
