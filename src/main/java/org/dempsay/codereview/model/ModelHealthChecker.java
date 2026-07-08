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
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class ModelHealthChecker {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  private ModelHealthChecker() {
  }

  public static ExceptionalResponse<HealthReport> check(final ModelConfig model) {
    if (model.isOllama()) {
      return checkOllama(model);
    }
    if (model.isOpenRouter()) {
      return checkOpenRouter(model);
    }
    return ExceptionalSupport.fail(
        new IllegalArgumentException(
            "Health check is only supported for ollama and openrouter providers (got: " + model.provider() + ")"
        )
    );
  }

  private static ExceptionalResponse<HealthReport> checkOllama(final ModelConfig model) {
    final URI tagsUri = URI.create(model.resolveBaseUrl() + "/api/tags");
    final HttpRequest request = HttpRequest.newBuilder(tagsUri)
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build();

    return ExceptionalSupport.supply(() -> HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()))
        .chain((listener, response) -> {
          if (response.statusCode() != 200) {
            return ExceptionalSupport.fail(
                listener,
                new IllegalStateException(
                    "Ollama health check failed: HTTP " + response.statusCode() + " from " + tagsUri
                )
            );
          }
          return ExceptionalSupport.supply(() -> MAPPER.readTree(response.body()))
              .chain((parseListener, root) -> validateOllamaModel(model, root, listener), listener);
        });
  }

  private static ExceptionalResponse<HealthReport> validateOllamaModel(
      final ModelConfig model,
      final JsonNode root,
      final ExceptionalListener listener
  ) {
    final JsonNode models = root.path("models");
    if (!models.isArray()) {
      return ExceptionalSupport.fail(
          listener,
          new IllegalStateException("Ollama /api/tags response did not include a models array")
      );
    }

    for (final JsonNode entry : models) {
      final String modelName = entry.path("name").asText("");
      if (modelName.equals(model.name()) || modelName.startsWith(model.name() + ":")) {
        return ExceptionalResponse.success(new HealthReport(
            model.provider(),
            model.name(),
            model.resolveBaseUrl(),
            "Ollama is reachable and model is available"
        ));
      }
    }

    return ExceptionalSupport.fail(
        listener,
        new IllegalStateException(
            "Ollama is reachable but model '" + model.name() + "' was not found. "
                + "Run: ollama pull " + model.name()
        )
    );
  }

  private static ExceptionalResponse<HealthReport> checkOpenRouter(final ModelConfig model) {
    final String apiKey = ChatModelFactory.requireApiKey(model);
    final URI modelsUri = URI.create(model.resolveBaseUrl() + "/models");
    final HttpRequest request = HttpRequest.newBuilder(modelsUri)
        .timeout(Duration.ofSeconds(10))
        .header("Authorization", "Bearer " + apiKey)
        .GET()
        .build();

    return ExceptionalSupport.supply(() -> HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()))
        .chain((listener, response) -> {
          if (response.statusCode() != 200) {
            return ExceptionalSupport.fail(
                listener,
                new IllegalStateException(
                    "OpenRouter health check failed: HTTP " + response.statusCode() + " from " + modelsUri
                )
            );
          }
          return ExceptionalSupport.supply(() -> MAPPER.readTree(response.body()))
              .chain((parseListener, root) -> validateOpenRouterModel(model, root, listener), listener);
        });
  }

  private static ExceptionalResponse<HealthReport> validateOpenRouterModel(
      final ModelConfig model,
      final JsonNode root,
      final ExceptionalListener listener
  ) {
    final JsonNode models = root.path("data");
    if (!models.isArray()) {
      return ExceptionalSupport.fail(
          listener,
          new IllegalStateException("OpenRouter /models response did not include a data array")
      );
    }

    for (final JsonNode entry : models) {
      final String modelId = entry.path("id").asText("");
      if (modelId.equals(model.name())) {
        return ExceptionalResponse.success(new HealthReport(
            model.provider(),
            model.name(),
            model.resolveBaseUrl(),
            "OpenRouter is reachable and model is available"
        ));
      }
    }

    return ExceptionalSupport.fail(
        listener,
        new IllegalStateException(
            "OpenRouter is reachable but model '" + model.name() + "' was not found in /models"
        )
    );
  }

  public record HealthReport(String provider, String modelName, String baseUrl, String message) {
  }
}