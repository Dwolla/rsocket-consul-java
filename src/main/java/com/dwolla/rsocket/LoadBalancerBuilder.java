package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.AsyncHttpClientImpl;
import io.rsocket.client.LoadBalancedRSocketMono;
import org.asynchttpclient.AsyncHttpClient;

import java.time.Duration;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

public class LoadBalancerBuilder {
  private String consulHost = "http://localhost:8500";
  private int requestTimeout = (int) Duration.ofSeconds(90).toMillis();

  public LoadBalancerBuilder withConsul(String consul) {
    this.consulHost = consul;
    return this;
  }

  public LoadBalancedRSocketMono build(String service) {
    AsyncHttpClient asyncHttpClient =
        asyncHttpClient(
            config().setRequestTimeout(requestTimeout).setReadTimeout(requestTimeout).build());

    ConsulRSocketLoadBalancerFactory factory =
        new ConsulRSocketLoadBalancerFactory(
            new ConsulHealthStream(new AsyncHttpClientImpl(asyncHttpClient), consulHost));

    return factory.create(service);
  }
}
