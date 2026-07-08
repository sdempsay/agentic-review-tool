package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.rules.Rule;

/**
 * Estimates prompt overhead and diff budget from context size.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class PromptBudgetEstimator {

  static final double BYTES_PER_TOKEN = 3.5;
  static final double SAFETY_MARGIN = 0.10;

  private PromptBudgetEstimator() {
  }

  /**
   * Estimates ruleset prompt overhead in bytes.
   * 
   * @param rule the rule
   * @return the result
   * @since 1.0.0
 */
  public static int rulesetOverheadBytes(final Rule rule) {
    return rulesetOverheadBytes(rule, ReviewContentMode.DIFF, ReviewPromptSupplements.empty());
  }

  /**
   * Estimates ruleset prompt overhead in bytes.
   * 
   * @param rule the rule
   * @param contentMode the contentMode
   * @return the result
   * @since 1.0.0
 */
  public static int rulesetOverheadBytes(final Rule rule, final ReviewContentMode contentMode) {
    return rulesetOverheadBytes(rule, contentMode, ReviewPromptSupplements.empty());
  }

  /**
   * Estimates ruleset prompt overhead in bytes.
   * 
   * @param rule the rule
   * @param contentMode the contentMode
   * @param supplements the supplements
   * @return the result
   * @since 1.0.0
 */
  public static int rulesetOverheadBytes(
      final Rule rule,
      final ReviewContentMode contentMode,
      final ReviewPromptSupplements supplements
  ) {
    return ReviewPromptBuilder.buildForRuleset(rule, List.of(), contentMode, supplements).length();
  }

  /**
   * Estimates general fallback prompt overhead in bytes.
   * 
   * @return the result
   * @since 1.0.0
 */
  public static int generalOverheadBytes() {
    return generalOverheadBytes(ReviewContentMode.DIFF, ReviewPromptSupplements.empty());
  }

  /**
   * Estimates general fallback prompt overhead in bytes.
   * 
   * @param contentMode the contentMode
   * @return the result
   * @since 1.0.0
 */
  public static int generalOverheadBytes(final ReviewContentMode contentMode) {
    return generalOverheadBytes(contentMode, ReviewPromptSupplements.empty());
  }

  /**
   * Estimates general fallback prompt overhead in bytes.
   * 
   * @param contentMode the contentMode
   * @param supplements the supplements
   * @return the result
   * @since 1.0.0
 */
  public static int generalOverheadBytes(
      final ReviewContentMode contentMode,
      final ReviewPromptSupplements supplements
  ) {
    return ReviewPromptBuilder.buildGeneralFallback(List.of(), contentMode, supplements).length();
  }

  /**
   * Max combined diff bytes that fit in the model context after overhead, generation reserve, and margin.
   * @since 1.0.0
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

  /**
   * Returns total prompt size from overhead and diff bytes.
   * 
   * @param overheadBytes the overheadBytes
   * @param diffBytes the diffBytes
   * @return the result
   * @since 1.0.0
 */
  public static int totalPromptBytes(final int overheadBytes, final int diffBytes) {
    return overheadBytes + diffBytes;
  }
}