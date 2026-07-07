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
    return build(config, contextTokens, PromptBudgetEstimator.rulesetOverheadBytes(rule));
  }

  public static AgentBatchLimits forGeneral(final AppConfig config, final int contextTokens) {
    return build(config, contextTokens, PromptBudgetEstimator.generalOverheadBytes());
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
        config.maxTokens(),
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