package org.dempsay.codereview.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.Test;

public class LlmTokenUsageExtractorTest {

  @Test
  public void normalizeComputesTotalWhenMissing() {
    final TokenUsage usage = LlmTokenUsageExtractor.normalize(new TokenUsage(100, 50, null));

    assertEquals(100, LlmTokenUsageExtractor.orZero(usage.inputTokenCount()));
    assertEquals(50, LlmTokenUsageExtractor.orZero(usage.outputTokenCount()));
    assertEquals(150, LlmTokenUsageExtractor.orZero(usage.totalTokenCount()));
  }

  @Test
  public void fromReturnsZerosWhenResponseMissingUsage() {
    final TokenUsage usage = LlmTokenUsageExtractor.from((ChatResponse) null);

    assertFalse(LlmTokenUsageExtractor.hasUsage(usage));
    assertEquals(0, LlmTokenUsageExtractor.orZero(usage.totalTokenCount()));
  }

  @Test
  public void hasUsageDetectsNonZeroCounts() {
    assertTrue(LlmTokenUsageExtractor.hasUsage(new TokenUsage(1, 0, 1)));
    assertFalse(LlmTokenUsageExtractor.hasUsage(new TokenUsage(0, 0, 0)));
  }
}