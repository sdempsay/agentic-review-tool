package org.dempsay.codereview.config;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import org.junit.Test;

public class ModelConfigTest {

  @Test
  public void resolveDefaultsForMissingOptionalFields() {
    final ModelConfig model = new ModelConfig("ollama", "qwen3", 0.2, null, 0);

    assertEquals("http://localhost:11434", model.resolveBaseUrl());
    assertEquals(Duration.ofSeconds(300), model.resolveTimeout());
  }

  @Test
  public void resolveConfiguredOptionalFields() {
    final ModelConfig model = new ModelConfig("ollama", "qwen3", 0.1, "http://ollama:11434", 60);

    assertEquals("http://ollama:11434", model.resolveBaseUrl());
    assertEquals(Duration.ofSeconds(60), model.resolveTimeout());
  }
}