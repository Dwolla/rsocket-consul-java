package com.dwolla.rsocket;

import io.rsocket.RSocketFactory;
import io.rsocket.client.filter.RSocketSupplier;
import io.rsocket.transport.netty.client.TcpClientTransport;

class AddressRSocketSupplierFactory {
  public RSocketSupplier create(Address address) {
    return new AddressRSocketSupplier(
        () ->
            RSocketFactory.connect()
                .transport(TcpClientTransport.create(address.getHost(), address.getPort()))
                .start(),
        address);
  }
}
