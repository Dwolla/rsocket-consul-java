# rsocket-consul-java
A library for streaming healthy service instances in Consul to the RSocket load balancer


## Usage
```java
import com.dwolla.rsocket.LoadBalancerBuilder;

// Create a LoadBalancedRSocketMono for your service
LoadBalancedRSocketMono rSocket = LoadBalancerBuilder.build("CONSUL_SERVICE_NAME");

// Then use the resulting RSocket like you would a single instance
rSocket.map(r -> r.fireAndForget(DefaultPayload.create("do something")))
```
