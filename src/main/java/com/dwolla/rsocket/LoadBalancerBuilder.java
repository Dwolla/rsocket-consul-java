package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.AsyncHttpClientImpl;
import com.dwolla.rsocket.consul.HealthPoller;
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

    HealthPoller poller = new HealthPoller(new AsyncHttpClientImpl(asyncHttpClient), consulHost);
    ConsulRSocketLoadBalancerFactory factory = new ConsulRSocketLoadBalancerFactory(poller);

    return factory.create(service);
  }
}
