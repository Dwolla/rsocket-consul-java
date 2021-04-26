package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.HttpClient;
import io.rsocket.transport.netty.server.CloseableChannel;

import java.util.Objects;

public class NamedServerAndClient {
    final String name;
    final CloseableChannel server;
    final HttpClient client;

    public NamedServerAndClient(final String name, CloseableChannel server, HttpClient client) {
        this.name = name;
        this.server = server;
        this.client = client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedServerAndClient that = (NamedServerAndClient) o;
        return Objects.equals(name, that.name) && Objects.equals(server, that.server) && Objects.equals(client, that.client);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, server, client);
    }
}
