package com.dwolla.rsocket;

import io.rsocket.RSocket;
import io.rsocket.client.filter.RSocketSupplier;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public class AddressRSocketSupplier extends RSocketSupplier {
  private Address address;

  protected AddressRSocketSupplier(Supplier<Mono<RSocket>> rSocketSupplier, Address address) {
    super(rSocketSupplier);
    this.address = address;
  }

  public Address getAddress() {
    return address;
  }

  @Override
  public int hashCode() {
    return address.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AddressRSocketSupplier) {
      return ((AddressRSocketSupplier) obj).getAddress().equals(getAddress());
    }

    return false;
  }
}
