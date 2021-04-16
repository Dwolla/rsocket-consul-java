# rsocket-consul-java
A library for streaming healthy service instances in Consul to the [RSocket](https://github.com/rsocket/rsocket-java) Java load balancer.

![Dwolla/rsocket-consul-java CI](https://github.com/Dwolla/rsocket-consul-java/actions/workflows/ci.yml/badge.svg)
[![license](https://img.shields.io/github/license/Dwolla/rsocket-consul-java.svg?style=flat-square)]()

## Usage
```java
import com.dwolla.rsocket.LoadBalancerBuilder;

// Create a LoadBalancedRSocketMono for your service
LoadBalancedRSocketMono rSocket = LoadBalancerBuilder.build("CONSUL_SERVICE_NAME");

// Then use the resulting RSocket like you would a single instance
rSocket.map(r -> r.fireAndForget(DefaultPayload.create("do something")))
```
