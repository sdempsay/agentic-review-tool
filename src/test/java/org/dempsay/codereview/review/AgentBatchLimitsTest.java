package org.dempsay.codereview.review;

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.config.ModelConfig;
import org.dempsay.codereview.rules.Rule;
import org.junit.Test;

public class AgentBatchLimitsTest {

  @Test
  public void forRulesetUsesSoftTargetAndLargeHardCapFromOllamaContext() {
    final AppConfig config = new AppConfig(
        new ModelConfig("ollama", "qwen3", 0.2, null, 0),
        Path.of("/tmp"),
        8000,
        512,
        256,
        0,
        List.of()
    );
    final Rule rule = new Rule("java-general", null, List.of("**/*.java"), "Rules");

    final AgentBatchLimits limits = AgentBatchLimits.forRuleset(config, 262144, rule);

    assertTrue(limits.softDiffBytes() == 256 * 1024);
    assertTrue(limits.hardDiffBytes() > limits.softDiffBytes());
  }

  @Test
  public void fromLegacyCapsUsesSoftAsHardWhenNoContext() {
    final AgentBatchLimits limits = AgentBatchLimits.fromLegacyCaps(256, 0, 100);

    assertTrue(limits.softDiffBytes() == 256 * 1024);
    assertTrue(limits.hardDiffBytes() == 256 * 1024);
  }
}