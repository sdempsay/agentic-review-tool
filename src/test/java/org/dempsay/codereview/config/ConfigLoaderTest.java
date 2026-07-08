package org.dempsay.codereview.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.junit.Test;

public class ConfigLoaderTest {

  @Test
  public void loadFromExplicitPath() throws Exception {
    final Path config = Files.createTempFile("code-review-config", ".json");
    Files.writeString(
        config,
        """
        {
          "model": {
            "provider": "ollama",
            "name": "llama3.2",
            "temperature": 0.5,
            "baseUrl": "http://127.0.0.1:11434",
            "timeoutSeconds": 90
          },
          "rulesDir": "~/custom-rules",
          "maxTokens": 4096
        }
        """
    );

    final AppConfig appConfig = ExceptionalSupport.response(ConfigLoader.load(config));

    assertEquals("ollama", appConfig.model().provider());
    assertEquals("llama3.2", appConfig.model().name());
    assertEquals(0.5, appConfig.model().temperature(), 0.001);
    assertEquals("http://127.0.0.1:11434", appConfig.model().baseUrl());
    assertEquals(90, appConfig.model().timeoutSeconds());
    assertEquals(Path.of(System.getProperty("user.home"), "custom-rules"), appConfig.rulesDir());
    assertEquals(4096, appConfig.maxTokens());
    assertEquals(4096, appConfig.reviewMaxTokens());
    assertEquals(4096, appConfig.resolvedReviewMaxTokens());
    assertEquals(512, appConfig.maxDiffKb());
    assertEquals(256, appConfig.maxAgentDiffKb());
    assertEquals(0, appConfig.maxFilesPerAgent());
    assertTrue(appConfig.repoExcludeExtensions().isEmpty());
  }

  @Test
  public void loadRepoExcludeExtensionsFromConfig() throws Exception {
    final Path config = Files.createTempFile("code-review-repo-excludes", ".json");
    Files.writeString(
        config,
        """
        {
          "model": {
            "provider": "ollama",
            "name": "llama3.2",
            "temperature": 0.2,
            "baseUrl": "http://127.0.0.1:11434",
            "timeoutSeconds": 90
          },
          "rulesDir": "~/custom-rules",
          "repoExcludeExtensions": [".xml", "yaml", ".yml"]
        }
        """
    );

    final AppConfig appConfig = ExceptionalSupport.response(ConfigLoader.load(config));

    assertEquals(List.of(".xml", "yaml", ".yml"), appConfig.repoExcludeExtensions());
  }

  @Test
  public void loadOpenRouterApiKeyFromConfig() throws Exception {
    final Path config = Files.createTempFile("code-review-openrouter-config", ".json");
    Files.writeString(
        config,
        """
        {
          "model": {
            "provider": "openrouter",
            "name": "anthropic/claude-sonnet-4",
            "temperature": 0.2,
            "apiKey": "sk-or-test"
          },
          "rulesDir": "~/custom-rules"
        }
        """
    );

    final AppConfig appConfig = ExceptionalSupport.response(ConfigLoader.load(config));

    assertEquals("openrouter", appConfig.model().provider());
    assertEquals("anthropic/claude-sonnet-4", appConfig.model().name());
    assertEquals("sk-or-test", appConfig.model().apiKey());
    assertEquals("https://openrouter.ai/api/v1", appConfig.model().resolveBaseUrl());
  }

  @Test
  public void loadBundledDefaultsWhenNoExplicitPathAndNoUserConfig() {
    final AppConfig appConfig = ExceptionalSupport.response(ConfigLoader.load(null));

    assertEquals("ollama", appConfig.model().provider());
    assertEquals("qwen3-coder-next-256k:latest", appConfig.model().name());
    assertEquals(Path.of("rules"), appConfig.rulesDir());
    assertEquals(24000, appConfig.maxTokens());
    assertEquals(4096, appConfig.reviewMaxTokens());
    assertEquals(4096, appConfig.resolvedReviewMaxTokens());
    assertEquals(512, appConfig.maxDiffKb());
    assertEquals(256, appConfig.maxAgentDiffKb());
    assertEquals(0, appConfig.maxFilesPerAgent());
    assertTrue(appConfig.repoExcludeExtensions().isEmpty());
  }

  @Test
  public void expandHomeHandlesTildeOnly() {
    assertEquals(Path.of(System.getProperty("user.home")), ConfigLoader.expandHome("~"));
  }
}