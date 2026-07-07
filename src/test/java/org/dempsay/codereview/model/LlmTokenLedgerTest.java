package org.dempsay.codereview.model;

import static org.junit.Assert.assertTrue;

import dev.langchain4j.model.output.TokenUsage;
import org.dempsay.codereview.config.ModelConfig;
import org.junit.Test;

public class LlmTokenLedgerTest {

  @Test
  public void aggregatesCallsAndFormatsReportSection() {
    final LlmTokenLedger ledger = new LlmTokenLedger();
    ledger.record("java-general", new TokenUsage(1200, 340, 1540));
    ledger.record("summarize", new TokenUsage(800, 220, 1020));

    final ModelConfig model = new ModelConfig("ollama", "qwen3", 0.2, null, 0, null);
    final String section = ledger.formatReportSection(model);

    assertTrue(section.contains("--- Token Usage ---"));
    assertTrue(section.contains("Provider: ollama"));
    assertTrue(section.contains("2000 input, 560 output, 2560 total"));
    assertTrue(section.contains("java-general: 1200 in / 340 out / 1540 total"));
    assertTrue(section.contains("summarize: 800 in / 220 out / 1020 total"));
  }
}