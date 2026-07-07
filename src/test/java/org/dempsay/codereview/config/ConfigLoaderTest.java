package org.dempsay.codereview.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
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
    assertEquals(512, appConfig.maxDiffKb());
  }

  @Test
  public void loadBundledDefaultsWhenNoExplicitPathAndNoUserConfig() {
    final AppConfig appConfig = ExceptionalSupport.response(ConfigLoader.load(null));

    assertEquals("ollama", appConfig.model().provider());
    assertEquals("qwen3.6-35b-mlx-256k:latest", appConfig.model().name());
    assertTrue(appConfig.rulesDir().endsWith(".grok/rules"));
    assertEquals(8000, appConfig.maxTokens());
    assertEquals(512, appConfig.maxDiffKb());
  }

  @Test
  public void expandHomeHandlesTildeOnly() {
    assertEquals(Path.of(System.getProperty("user.home")), ConfigLoader.expandHome("~"));
  }
}