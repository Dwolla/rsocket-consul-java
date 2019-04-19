package com.dwolla.rsocket.consul;

import java.util.concurrent.CompletableFuture;

public interface HttpClient {
  CompletableFuture<SimpleResponse> get(String url);
}

