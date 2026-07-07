package org.dempsay.codereview.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResource;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class ConfigLoader {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Path DEFAULT_USER_CONFIG =
      Paths.get(System.getProperty("user.home"), ".code-review", "config.json");

  private ConfigLoader() {
  }

  public static ExceptionalResponse<AppConfig> load(final Path explicitConfigPath) {
    return ExceptionalSupport.supply(() -> {
      final Path configPath = resolveConfigPath(explicitConfigPath);
      final JsonNode root = ExceptionalSupport.response(readConfig(configPath));
      return toAppConfig(root);
    });
  }

  public static String describeSource(final Path explicitConfigPath) {
    if (explicitConfigPath != null) {
      return explicitConfigPath.toAbsolutePath().toString();
    }
    if (Files.isRegularFile(DEFAULT_USER_CONFIG)) {
      return DEFAULT_USER_CONFIG.toAbsolutePath().toString();
    }
    return "bundled classpath:/default-config.json (rebuild with mvn package after edits)";
  }

  private static Path resolveConfigPath(final Path explicitConfigPath) {
    if (explicitConfigPath != null) {
      return explicitConfigPath;
    }
    if (Files.isRegularFile(DEFAULT_USER_CONFIG)) {
      return DEFAULT_USER_CONFIG;
    }
    return null;
  }

  private static ExceptionalResponse<JsonNode> readConfig(final Path configPath) {
    if (configPath != null) {
      return ExceptionalResource.of(
          () -> Files.newBufferedReader(configPath),
          MAPPER::readTree
      ).execute();
    }

    return ExceptionalResource.of(
        ConfigLoader::openDefaultConfigStream,
        MAPPER::readTree
    ).execute();
  }

  private static InputStream openDefaultConfigStream() {
    final InputStream input = ConfigLoader.class.getResourceAsStream("/default-config.json");
    if (input == null) {
      throw new IllegalStateException("Bundled default-config.json is missing from the classpath");
    }
    return input;
  }

  private static AppConfig toAppConfig(final JsonNode root) {
    final JsonNode modelNode = root.path("model");
    final ModelConfig model = new ModelConfig(
        requiredText(modelNode, "provider"),
        requiredText(modelNode, "name"),
        modelNode.path("temperature").asDouble(0.2),
        optionalText(modelNode, "baseUrl"),
        modelNode.path("timeoutSeconds").asInt(0),
        optionalText(modelNode, "apiKey")
    );
    final String rulesDir = requiredText(root, "rulesDir");
    final int maxTokens = root.path("maxTokens").asInt(8000);
    final int maxDiffKb = root.path("maxDiffKb").asInt(512);
    final int maxAgentDiffKb = root.path("maxAgentDiffKb").asInt(256);
    final int maxFilesPerAgent = root.path("maxFilesPerAgent").asInt(0);
    final List<String> repoExcludeExtensions = readStringList(root, "repoExcludeExtensions");
    return new AppConfig(
        model,
        expandHome(rulesDir),
        maxTokens,
        maxDiffKb,
        maxAgentDiffKb,
        maxFilesPerAgent,
        repoExcludeExtensions
    );
  }

  private static List<String> readStringList(final JsonNode root, final String field) {
    final JsonNode node = root.path(field);
    if (!node.isArray()) {
      return List.of();
    }

    final List<String> values = new ArrayList<>();
    for (final JsonNode item : node) {
      if (item.isTextual() && !item.asText().isBlank()) {
        values.add(item.asText());
      }
    }
    return List.copyOf(values);
  }

  private static String optionalText(final JsonNode node, final String field) {
    final JsonNode value = node.path(field);
    return value.isTextual() ? value.asText() : null;
  }

  private static String requiredText(final JsonNode node, final String field) {
    final JsonNode value = node.path(field);
    if (!value.isTextual() || value.asText().isBlank()) {
      throw new IllegalArgumentException("Config field '" + field + "' is required");
    }
    return value.asText();
  }

  static Path expandHome(final String path) {
    if (path.startsWith("~/")) {
      return Paths.get(System.getProperty("user.home"), path.substring(2));
    }
    if ("~".equals(path)) {
      return Paths.get(System.getProperty("user.home"));
    }
    return Paths.get(path);
  }
}