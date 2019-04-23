package com.dwolla.rsocket;

import io.rsocket.client.filter.RSocketSupplier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressRSocketSupplierFactoryShould {
  @Test
  void createInstancesOfAddressRSocketSupplier() {
    Address addr1 = new Address("host1", 1234);
    RSocketSupplier supplier = new AddressRSocketSupplierFactory().create(addr1);

    assertTrue(supplier instanceof AddressRSocketSupplier);
    assertEquals(addr1, ((AddressRSocketSupplier) supplier).getAddress());
  }
}
