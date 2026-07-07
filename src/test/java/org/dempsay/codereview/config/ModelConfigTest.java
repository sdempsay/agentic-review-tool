package org.dempsay.codereview.config;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import org.junit.Test;

public class ModelConfigTest {

  @Test
  public void resolveDefaultsForMissingOptionalFields() {
    final ModelConfig model = new ModelConfig("ollama", "qwen3", 0.2, null, 0, null);

    assertEquals("http://localhost:11434", model.resolveBaseUrl());
    assertEquals(Duration.ofSeconds(300), model.resolveTimeout());
  }

  @Test
  public void resolveConfiguredOptionalFields() {
    final ModelConfig model = new ModelConfig("ollama", "qwen3", 0.1, "http://ollama:11434", 60, null);

    assertEquals("http://ollama:11434", model.resolveBaseUrl());
    assertEquals(Duration.ofSeconds(60), model.resolveTimeout());
  }

  @Test
  public void resolveOpenRouterDefaultsAndEnvApiKey() {
    final ModelConfig model = new ModelConfig("openrouter", "anthropic/claude-sonnet-4", 0.2, null, 0, null);

    assertEquals("https://openrouter.ai/api/v1", model.resolveBaseUrl());
    assertEquals(System.getenv("OPENROUTER_API_KEY"), model.resolveApiKey());
  }

  @Test
  public void resolveApiKeyFromEnvReference() {
    final ModelConfig model = new ModelConfig(
        "openrouter",
        "anthropic/claude-sonnet-4",
        0.2,
        null,
        0,
        "${OPENROUTER_API_KEY}"
    );

    assertEquals(System.getenv("OPENROUTER_API_KEY"), model.resolveApiKey());
  }
}