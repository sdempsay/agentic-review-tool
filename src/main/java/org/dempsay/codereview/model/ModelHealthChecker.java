package org.dempsay.codereview.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.dempsay.codereview.config.ModelConfig;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class ModelHealthChecker {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  private ModelHealthChecker() {
  }

  public static ExceptionalResponse<HealthReport> check(final ModelConfig model) {
    return ExceptionalSupport.supply(() -> checkRequired(model));
  }

  public static HealthReport checkRequired(final ModelConfig model) throws Exception {
    if (model.isOllama()) {
      return checkOllamaRequired(model);
    }
    if (model.isOpenRouter()) {
      return checkOpenRouterRequired(model);
    }
    throw new IllegalArgumentException(
        "Health check is only supported for ollama and openrouter providers (got: " + model.provider() + ")"
    );
  }

  private static HealthReport checkOllamaRequired(final ModelConfig model) throws Exception {
    final URI tagsUri = URI.create(model.resolveBaseUrl() + "/api/tags");
    final HttpRequest request = HttpRequest.newBuilder(tagsUri)
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build();
    final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "Ollama health check failed: HTTP " + response.statusCode() + " from " + tagsUri
      );
    }

    final JsonNode root = MAPPER.readTree(response.body());
    final JsonNode models = root.path("models");
    if (!models.isArray()) {
      throw new IllegalStateException("Ollama /api/tags response did not include a models array");
    }

    boolean modelAvailable = false;
    for (final JsonNode entry : models) {
      final String modelName = entry.path("name").asText("");
      if (modelName.equals(model.name()) || modelName.startsWith(model.name() + ":")) {
        modelAvailable = true;
        break;
      }
    }

    if (!modelAvailable) {
      throw new IllegalStateException(
          "Ollama is reachable but model '" + model.name() + "' was not found. "
              + "Run: ollama pull " + model.name()
      );
    }

    return new HealthReport(
        model.provider(),
        model.name(),
        model.resolveBaseUrl(),
        "Ollama is reachable and model is available"
    );
  }

  private static HealthReport checkOpenRouterRequired(final ModelConfig model) throws Exception {
    final String apiKey = ChatModelFactory.requireApiKey(model);
    final URI modelsUri = URI.create(model.resolveBaseUrl() + "/models");
    final HttpRequest request = HttpRequest.newBuilder(modelsUri)
        .timeout(Duration.ofSeconds(10))
        .header("Authorization", "Bearer " + apiKey)
        .GET()
        .build();
    final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "OpenRouter health check failed: HTTP " + response.statusCode() + " from " + modelsUri
      );
    }

    final JsonNode root = MAPPER.readTree(response.body());
    final JsonNode models = root.path("data");
    if (!models.isArray()) {
      throw new IllegalStateException("OpenRouter /models response did not include a data array");
    }

    boolean modelAvailable = false;
    for (final JsonNode entry : models) {
      final String modelId = entry.path("id").asText("");
      if (modelId.equals(model.name())) {
        modelAvailable = true;
        break;
      }
    }

    if (!modelAvailable) {
      throw new IllegalStateException(
          "OpenRouter is reachable but model '" + model.name() + "' was not found in /models"
      );
    }

    return new HealthReport(
        model.provider(),
        model.name(),
        model.resolveBaseUrl(),
        "OpenRouter is reachable and model is available"
    );
  }

  public record HealthReport(String provider, String modelName, String baseUrl, String message) {
  }
}