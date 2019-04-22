# rsocket-consul-java
A library for streaming healthy service instances in Consul to the RSocket load balancer

[![Build Status](https://travis-ci.org/Dwolla/rsocket-consul-java.svg?branch=master)](https://travis-ci.org/Dwolla/rsocket-consul-java)
[ ![Download](https://api.bintray.com/packages/dwolla/maven/rsocket-consul-java/images/download.svg) ](https://bintray.com/dwolla/maven/rsocket-consul-java/_latestVersion)


## Usage
```java
import com.dwolla.rsocket.LoadBalancerBuilder;

// Create a LoadBalancedRSocketMono for your service
LoadBalancedRSocketMono rSocket = LoadBalancerBuilder.build("CONSUL_SERVICE_NAME");

// Then use the resulting RSocket like you would a single instance
rSocket.map(r -> r.fireAndForget(DefaultPayload.create("do something")))
```
