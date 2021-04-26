package com.dwolla.rsocket;

import com.dwolla.rsocket.consul.HttpClient;
import com.dwolla.rsocket.consul.SimpleResponse;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.EmptyPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoadBalancerBuilderShould {
    private Queue<String> handlers;

    static String instanceOn(final InetSocketAddress address) {
        return String.format("{`Service`:{`Address`:`%s`,`Port`:%d}}", address.getHostString(), address.getPort()).replace('`', '"');
    }

    @BeforeEach
    void setup() {
        handlers = new ConcurrentLinkedQueue<>();
    }

    final HttpClient setupClientForService(final String service,
                                           final InetSocketAddress... addresses) {
        final String consulResponse = String.format("[%s]",
                Arrays.stream(addresses)
                        .map(LoadBalancerBuilderShould::instanceOn)
                        .collect(Collectors.joining(",")));

        final List<Map.Entry<String, String>> headers = new ArrayList<>(1);
        headers.add(new Map.Entry<String, String>() {
            @Override
            public String getKey() {
                return "X-Consul-Index";
            }

            @Override
            public String getValue() {
                return "1";
            }

            @Override
            public String setValue(String value) {
                throw new UnsupportedOperationException("setValue not supported");
            }
        });

        return url -> {
            if (url.equals(String.format("http://localhost:8500/v1/health/service/%s?passing=true&index=%d&wait=1m", service, 0))) {
                return CompletableFuture.completedFuture(new SimpleResponse(consulResponse, headers));
            } else {
                return Mono.delay(Duration.ofSeconds(60)).then(Mono.just(new SimpleResponse(consulResponse, headers))).toFuture();
            }
        };
    }

    static Mono<Payload> makeRequest(final NamedServerAndClient nsc) {
        return new LoadBalancerBuilder(nsc.client)
                .build(nsc.name)
                .requestResponse(Mono.just(EmptyPayload.INSTANCE))
                .flatMap(x -> {
                    nsc.server.dispose();
                    return nsc.server.onClose().thenReturn(x);
                })
                .onErrorResume(ex -> {
                    nsc.server.dispose();
                    return nsc.server.onClose().then(Mono.error(ex));
                });
    }

    Mono<CloseableChannel> startServer(final String service) {
        return RSocketServer.create((setupPayload, reactiveSocket) ->
                Mono.just(
                        new RSocket() {
                            @Override
                            @NonNull
                            public Mono<Payload> requestResponse(@NonNull final Payload p) {
                                handlers.add(service);
                                return Mono.just(EmptyPayload.INSTANCE);
                            }
                        })
        )
                .bind(TcpServerTransport.create(0));
    }

    Mono<NamedServerAndClient> startServerWithClient(final String service) {
        return startServer(service)
                .flatMap(server ->
                        Mono.just(new NamedServerAndClient(service, server, setupClientForService(service, server.address()))));
    }

    void assertHandlersContains(String s) {
        assertTrue(handlers.contains(s), () -> String.format("%s did not contain %s", handlers, s));
    }

    @RepeatedTest(value = 100)
    void workWithSingleInstance() {
        final String serviceName = "service1";
        startServerWithClient(serviceName)
                .flatMap(LoadBalancerBuilderShould::makeRequest)
                .block(Duration.ofSeconds(2));

        assertHandlersContains(serviceName);
    }

    @RepeatedTest(value = 100)
    void workWithMultipleInstances() {
        final List<String> servers = IntStream.range(1, new Random().nextInt(10) + 1).boxed().map(i -> String.format("server%d", i)).collect(Collectors.toList());

        Mono.zip(servers.stream().map(this::startServerWithClient).collect(Collectors.toList()), LoadBalancerBuilderShould.<NamedServerAndClient>toList())
                .flatMap(serversAndClients ->
                        Mono.zip(serversAndClients.stream().map(LoadBalancerBuilderShould::makeRequest).collect(Collectors.toList()), Arrays::asList)
                )
                .block(Duration.ofSeconds(2));

        servers.forEach(this::assertHandlersContains);
    }

    private static <T> Function<Object[], List<T>> toList() {
        //noinspection unchecked
        return objects -> (List<T>) Arrays.stream(objects).collect(Collectors.toList());
    }
}
