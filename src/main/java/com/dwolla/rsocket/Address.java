package com.dwolla.rsocket;

import java.util.Objects;

public class Address {
  private final String host;
  private final Integer port;

  public Address(final String host, final Integer port) {
    this.host = host;
    this.port = port;
  }

  public int getPort() {
    return port;
  }

  public String getHost() {
    return host;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Address address = (Address) o;
    return Objects.equals(host, address.host) && Objects.equals(port, address.port);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port);
  }
}
