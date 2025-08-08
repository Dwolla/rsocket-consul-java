package com.dwolla.rsocket.consul;

import org.asynchttpclient.AsyncHttpClient;

import java.util.concurrent.CompletableFuture;

public class AsyncHttpClientImpl implements HttpClient {
  private final AsyncHttpClient client;

  public AsyncHttpClientImpl(AsyncHttpClient client) {
    this.client = client;
  }

  @Override
  public CompletableFuture<SimpleResponse> get(final String url) {
    return client
        .prepareGet(url)
        .execute()
        .toCompletableFuture()
        .thenApply(r -> new SimpleResponse(r.getResponseBody(), r.getHeaders().entries()));
  }

  @Override
  public void close() throws java.io.IOException {
    client.close();
  }
}
