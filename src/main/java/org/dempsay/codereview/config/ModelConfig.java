package org.dempsay.codereview.config;

import java.time.Duration;

public record ModelConfig(
    String provider,
    String name,
    double temperature,
    String baseUrl,
    int timeoutSeconds,
    String apiKey
) {

  private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
  private static final String DEFAULT_OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
  private static final String OPENROUTER_API_KEY_ENV = "OPENROUTER_API_KEY";
  private static final int DEFAULT_TIMEOUT_SECONDS = 300;

  public String resolveBaseUrl() {
    if (baseUrl != null && !baseUrl.isBlank()) {
      return baseUrl;
    }
    if (isOpenRouter()) {
      return DEFAULT_OPENROUTER_BASE_URL;
    }
    return DEFAULT_OLLAMA_BASE_URL;
  }

  public String resolveApiKey() {
    final String configured = resolveEnvReference(apiKey);
    if (configured != null && !configured.isBlank()) {
      return configured;
    }
    if (isOpenRouter()) {
      return System.getenv(OPENROUTER_API_KEY_ENV);
    }
    return null;
  }

  public Duration resolveTimeout() {
    final int seconds = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    return Duration.ofSeconds(seconds);
  }

  public boolean isOllama() {
    return "ollama".equalsIgnoreCase(provider);
  }

  public boolean isOpenRouter() {
    return "openrouter".equalsIgnoreCase(provider);
  }

  static String resolveEnvReference(final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    if (value.startsWith("${") && value.endsWith("}")) {
      final String envName = value.substring(2, value.length() - 1).trim();
      return System.getenv(envName);
    }
    return value;
  }
}