package com.dwolla.rsocket.consul;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface HttpClient extends AutoCloseable {
  CompletableFuture<SimpleResponse> get(String url);

  @Override
  default void close() throws IOException {

  }
}
