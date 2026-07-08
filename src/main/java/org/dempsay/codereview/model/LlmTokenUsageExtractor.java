package org.dempsay.codereview.model;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

/**
 * Extracts and normalizes token usage from chat responses.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class LlmTokenUsageExtractor {

  private LlmTokenUsageExtractor() {
  }

  /**
   * Extracts token usage from a chat response.
   * 
   * @param response the response
   * @return the result
   * @since 1.0.0
 */
  public static TokenUsage from(final ChatResponse response) {
    if (response == null || response.tokenUsage() == null) {
      return emptyUsage();
    }
    return normalize(response.tokenUsage());
  }

  /**
   * Normalizes token usage counts.
   * 
   * @param usage the usage
   * @return the result
   * @since 1.0.0
 */
  public static TokenUsage normalize(final TokenUsage usage) {
    if (usage == null) {
      return emptyUsage();
    }
    final int input = valueOrZero(usage.inputTokenCount());
    final int output = valueOrZero(usage.outputTokenCount());
    final int total = valueOrZero(usage.totalTokenCount());
    if (total > 0) {
      return new TokenUsage(input, output, total);
    }
    if (input > 0 || output > 0) {
      return new TokenUsage(input, output, input + output);
    }
    return emptyUsage();
  }

  /**
   * Returns whether token usage has any non-zero counts.
   * 
   * @param usage the usage
   * @return the result
   * @since 1.0.0
 */
  public static boolean hasUsage(final TokenUsage usage) {
    return valueOrZero(usage.inputTokenCount()) > 0
        || valueOrZero(usage.outputTokenCount()) > 0
        || valueOrZero(usage.totalTokenCount()) > 0;
  }

  private static TokenUsage emptyUsage() {
    return new TokenUsage(0, 0, 0);
  }

  static int orZero(final Integer value) {
    return value == null ? 0 : value;
  }

  private static int valueOrZero(final Integer value) {
    return orZero(value);
  }
}