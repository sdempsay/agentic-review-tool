package org.dempsay.codereview.review;

import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.rules.Rule;

/**
 * Soft and hard diff byte caps for agent review batches.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record AgentBatchLimits(
    int softDiffBytes,
    int hardDiffBytes,
    int maxFilesPerAgent,
    int promptOverheadBytes
) {

  /**
   * Creates batch limits for a ruleset review.
   * 
   * @param config the config
   * @param contextTokens the contextTokens
   * @param rule the rule
   * @return the result
   * @since 1.0.0
 */
  public static AgentBatchLimits forRuleset(
      final AppConfig config,
      final int contextTokens,
      final Rule rule
  ) {
    return forRuleset(config, contextTokens, rule, ReviewContentMode.DIFF);
  }

  /**
   * Creates batch limits for a ruleset review.
   * 
   * @param config the config
   * @param contextTokens the contextTokens
   * @param rule the rule
   * @param contentMode the contentMode
   * @return the result
   * @since 1.0.0
 */
  public static AgentBatchLimits forRuleset(
      final AppConfig config,
      final int contextTokens,
      final Rule rule,
      final ReviewContentMode contentMode
  ) {
    return forRuleset(config, contextTokens, rule, contentMode, ReviewPromptSupplements.empty());
  }

  /**
   * Creates batch limits for a ruleset review.
   * 
   * @param config the config
   * @param contextTokens the contextTokens
   * @param rule the rule
   * @param contentMode the contentMode
   * @param supplements the supplements
   * @return the result
   * @since 1.0.0
 */
  public static AgentBatchLimits forRuleset(
      final AppConfig config,
      final int contextTokens,
      final Rule rule,
      final ReviewContentMode contentMode,
      final ReviewPromptSupplements supplements
  ) {
    return build(config, contextTokens, PromptBudgetEstimator.rulesetOverheadBytes(rule, contentMode, supplements));
  }

  /**
   * Creates batch limits for a general fallback review.
   * 
   * @param config the config
   * @param contextTokens the contextTokens
   * @return the result
   * @since 1.0.0
 */
  public static AgentBatchLimits forGeneral(final AppConfig config, final int contextTokens) {
    return forGeneral(config, contextTokens, ReviewContentMode.DIFF);
  }

  /**
   * Creates batch limits for a general fallback review.
   * 
   * @param config the config
   * @param contextTokens the contextTokens
   * @param contentMode the contentMode
   * @return the result
   * @since 1.0.0
 */
  public static AgentBatchLimits forGeneral(
      final AppConfig config,
      final int contextTokens,
      final ReviewContentMode contentMode
  ) {
    return forGeneral(config, contextTokens, contentMode, ReviewPromptSupplements.empty());
  }

  /**
   * Creates batch limits for a general fallback review.
   * 
   * @param config the config
   * @param contextTokens the contextTokens
   * @param contentMode the contentMode
   * @param supplements the supplements
   * @return the result
   * @since 1.0.0
 */
  public static AgentBatchLimits forGeneral(
      final AppConfig config,
      final int contextTokens,
      final ReviewContentMode contentMode,
      final ReviewPromptSupplements supplements
  ) {
    return build(config, contextTokens, PromptBudgetEstimator.generalOverheadBytes(contentMode, supplements));
  }

  /**
   * Creates batch limits from legacy KB and file caps.
   * 
   * @param maxAgentDiffKb the maxAgentDiffKb
   * @param maxFilesPerAgent the maxFilesPerAgent
   * @param promptOverheadBytes the promptOverheadBytes
   * @return the result
   * @since 1.0.0
 */
  public static AgentBatchLimits fromLegacyCaps(
      final int maxAgentDiffKb,
      final int maxFilesPerAgent,
      final int promptOverheadBytes
  ) {
    final int soft = maxAgentDiffKb > 0 ? maxAgentDiffKb * 1024 : 0;
    final int hard = soft > 0 ? soft : Integer.MAX_VALUE;
    return new AgentBatchLimits(soft, hard, maxFilesPerAgent, promptOverheadBytes);
  }

  private static AgentBatchLimits build(
      final AppConfig config,
      final int contextTokens,
      final int overheadBytes
  ) {
    final int soft = config.maxAgentDiffKb() > 0 ? config.maxAgentDiffKb() * 1024 : 0;
    final int hardFromContext = PromptBudgetEstimator.diffBudgetBytes(
        contextTokens,
        config.resolvedReviewMaxTokens(),
        overheadBytes
    );
    final int hard = hardFromContext > 0 ? hardFromContext : (soft > 0 ? soft : Integer.MAX_VALUE);
    return new AgentBatchLimits(soft, hard, config.maxFilesPerAgent(), overheadBytes);
  }

  /**
   * Returns whether a soft diff byte cap is configured.
   * 
   * @return the result
   * @since 1.0.0
 */
  public boolean hasSoftCap() {
    return softDiffBytes > 0;
  }

  /**
   * Returns whether a hard diff byte cap is configured.
   * 
   * @return the result
   * @since 1.0.0
 */
  public boolean hasHardCap() {
    return hardDiffBytes > 0 && hardDiffBytes < Integer.MAX_VALUE;
  }

  /**
   * Returns whether diff bytes exceed the hard cap.
   * 
   * @param diffBytes the diffBytes
   * @return the result
   * @since 1.0.0
 */
  public boolean exceedsHardCap(final int diffBytes) {
    return hasHardCap() && diffBytes > hardDiffBytes;
  }
}