package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.rules.Rule;

public final class PromptBudgetEstimator {

  static final double BYTES_PER_TOKEN = 3.5;
  static final double SAFETY_MARGIN = 0.10;

  private PromptBudgetEstimator() {
  }

  public static int rulesetOverheadBytes(final Rule rule) {
    return ReviewPromptBuilder.buildForRuleset(rule, List.of()).length();
  }

  public static int generalOverheadBytes() {
    return ReviewPromptBuilder.buildGeneralFallback(List.of()).length();
  }

  /**
   * Max combined diff bytes that fit in the model context after overhead, generation reserve, and margin.
   */
  public static int diffBudgetBytes(final int contextTokens, final int maxTokens, final int overheadBytes) {
    if (contextTokens <= 0) {
      return 0;
    }
    final int reservedTokens = Math.max(0, maxTokens);
    final int promptTokens = contextTokens - reservedTokens;
    if (promptTokens <= 0) {
      return 0;
    }
    final int safePromptTokens = (int) Math.floor(promptTokens * (1.0 - SAFETY_MARGIN));
    final int totalPromptBytes = (int) Math.floor(safePromptTokens * BYTES_PER_TOKEN);
    return Math.max(0, totalPromptBytes - overheadBytes);
  }

  public static int totalPromptBytes(final int overheadBytes, final int diffBytes) {
    return overheadBytes + diffBytes;
  }
}