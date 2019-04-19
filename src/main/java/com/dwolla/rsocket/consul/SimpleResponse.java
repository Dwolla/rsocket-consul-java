package com.dwolla.rsocket.consul;

import java.util.List;
import java.util.Map;

public class SimpleResponse {
  private String body;
  private List<Map.Entry<String, String>> headers;

  public SimpleResponse(final String body, final List<Map.Entry<String, String>> headers) {
    this.body = body;
    this.headers = headers;
  }

  public String getBody() {
    return body;
  }

  public List<Map.Entry<String, String>> getHeaders() {
    return headers;
  }
}
