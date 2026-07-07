package org.dempsay.codereview.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dempsay.codereview.config.ModelConfig;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class OllamaModelInspector {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Pattern NUM_CTX_PATTERN = Pattern.compile("num_ctx\\s+(\\d+)");
  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  private OllamaModelInspector() {
  }

  public static ExceptionalResponse<Integer> fetchContextTokens(final ModelConfig model) {
    return ExceptionalSupport.supply(() -> fetchContextTokensRequired(model));
  }

  /**
   * Returns configured Ollama context tokens ({@code num_ctx}), or {@code 0} when unavailable.
   */
  public static int resolveContextTokens(final ModelConfig model) {
    if (!"ollama".equalsIgnoreCase(model.provider())) {
      return 0;
    }
    final ExceptionalResponse<Integer> response = fetchContextTokens(model);
    return response.wasError() ? 0 : response.response();
  }

  public static int fetchContextTokensRequired(final ModelConfig model) throws Exception {
    final URI showUri = URI.create(model.resolveBaseUrl() + "/api/show");
    final String requestBody = MAPPER.writeValueAsString(Map.of("model", model.name()));
    final HttpRequest request = HttpRequest.newBuilder(showUri)
        .timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();
    final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "Ollama /api/show failed: HTTP " + response.statusCode() + " from " + showUri
      );
    }
    return parseContextTokens(MAPPER.readTree(response.body()));
  }

  static int parseContextTokens(final JsonNode root) {
    final int fromParameters = parseNumCtxFromParameters(root.path("parameters").asText(""));
    if (fromParameters > 0) {
      return fromParameters;
    }
    return parseContextLengthFromModelInfo(root.path("model_info"));
  }

  private static int parseNumCtxFromParameters(final String parameters) {
    if (parameters == null || parameters.isBlank()) {
      return 0;
    }
    final Matcher matcher = NUM_CTX_PATTERN.matcher(parameters);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }
    return 0;
  }

  private static int parseContextLengthFromModelInfo(final JsonNode modelInfo) {
    if (modelInfo == null || !modelInfo.isObject()) {
      return 0;
    }
    int best = 0;
    final Iterator<Map.Entry<String, JsonNode>> fields = modelInfo.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      if (field.getKey().endsWith(".context_length") && field.getValue().isNumber()) {
        best = Math.max(best, field.getValue().asInt());
      }
    }
    return best;
  }
}