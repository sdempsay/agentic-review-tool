package org.dempsay.codereview.review;

import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.rules.Rule;

public record AgentBatchLimits(
    int softDiffBytes,
    int hardDiffBytes,
    int maxFilesPerAgent,
    int promptOverheadBytes
) {

  public static AgentBatchLimits forRuleset(
      final AppConfig config,
      final int contextTokens,
      final Rule rule
  ) {
    return forRuleset(config, contextTokens, rule, ReviewContentMode.DIFF);
  }

  public static AgentBatchLimits forRuleset(
      final AppConfig config,
      final int contextTokens,
      final Rule rule,
      final ReviewContentMode contentMode
  ) {
    return forRuleset(config, contextTokens, rule, contentMode, ReviewPromptSupplements.empty());
  }

  public static AgentBatchLimits forRuleset(
      final AppConfig config,
      final int contextTokens,
      final Rule rule,
      final ReviewContentMode contentMode,
      final ReviewPromptSupplements supplements
  ) {
    return build(config, contextTokens, PromptBudgetEstimator.rulesetOverheadBytes(rule, contentMode, supplements));
  }

  public static AgentBatchLimits forGeneral(final AppConfig config, final int contextTokens) {
    return forGeneral(config, contextTokens, ReviewContentMode.DIFF);
  }

  public static AgentBatchLimits forGeneral(
      final AppConfig config,
      final int contextTokens,
      final ReviewContentMode contentMode
  ) {
    return forGeneral(config, contextTokens, contentMode, ReviewPromptSupplements.empty());
  }

  public static AgentBatchLimits forGeneral(
      final AppConfig config,
      final int contextTokens,
      final ReviewContentMode contentMode,
      final ReviewPromptSupplements supplements
  ) {
    return build(config, contextTokens, PromptBudgetEstimator.generalOverheadBytes(contentMode, supplements));
  }

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

  public boolean hasSoftCap() {
    return softDiffBytes > 0;
  }

  public boolean hasHardCap() {
    return hardDiffBytes > 0 && hardDiffBytes < Integer.MAX_VALUE;
  }

  public boolean exceedsHardCap(final int diffBytes) {
    return hasHardCap() && diffBytes > hardDiffBytes;
  }
}