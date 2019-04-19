package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.AsyncHttpClientImpl;
import io.rsocket.client.LoadBalancedRSocketMono;

import java.time.Duration;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class LoadBalancerBuilder {
  private String consulHost = "http://localhost:8500";
  private int requestTimeout = (int) Duration.ofSeconds(90).toMillis();

  public LoadBalancerBuilder withConsul(String consul) {
    this.consulHost = consul;
    return this;
  }

  public LoadBalancedRSocketMono build(String service) {
    ConsulRSocketLoadBalancerFactory factory =
        new ConsulRSocketLoadBalancerFactory(
            new ConsulHealthStream(new AsyncHttpClientImpl(asyncHttpClient()), consulHost));

    return factory.create(service);
  }
}
