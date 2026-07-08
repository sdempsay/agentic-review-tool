package org.dempsay.codereview.config;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public class AppConfigTest {

  @Test
  public void resolvedReviewMaxTokensUsesExplicitCapWhenSet() {
    final AppConfig config = new AppConfig(
        new ModelConfig("ollama", "qwen3", 0.2, null, 0, null),
        Path.of("rules"),
        24000,
        4096,
        512,
        256,
        0,
        List.of()
    );

    assertEquals(4096, config.resolvedReviewMaxTokens());
  }

  @Test
  public void resolvedReviewMaxTokensFallsBackToMaxTokensWhenUnset() {
    final AppConfig config = new AppConfig(
        new ModelConfig("ollama", "qwen3", 0.2, null, 0, null),
        Path.of("rules"),
        8000,
        0,
        512,
        256,
        0,
        List.of()
    );

    assertEquals(8000, config.resolvedReviewMaxTokens());
  }
}