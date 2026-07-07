package org.dempsay.codereview.config;

import java.time.Duration;

public record ModelConfig(
    String provider,
    String name,
    double temperature,
    String baseUrl,
    int timeoutSeconds
) {

  private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
  private static final int DEFAULT_TIMEOUT_SECONDS = 300;

  public String resolveBaseUrl() {
    if (baseUrl != null && !baseUrl.isBlank()) {
      return baseUrl;
    }
    return DEFAULT_OLLAMA_BASE_URL;
  }

  public Duration resolveTimeout() {
    final int seconds = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    return Duration.ofSeconds(seconds);
  }
}