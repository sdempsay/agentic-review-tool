package org.dempsay.codereview.config;

import java.nio.file.Path;
import java.util.List;

/**
 * Application configuration loaded from config.json.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record AppConfig(
    ModelConfig model,
    Path rulesDir,
    int maxTokens,
    int reviewMaxTokens,
    int maxDiffKb,
    int maxAgentDiffKb,
    int maxFilesPerAgent,
    List<String> repoExcludeExtensions
) {
  /**
   * Defensive copy of exclude-extension list.
   *
   * @since 1.0.0
   */
  public AppConfig {
    repoExcludeExtensions = List.copyOf(repoExcludeExtensions == null ? List.of() : repoExcludeExtensions);
  }

  /**
   * Max tokens the review pipeline may generate per LLM call (agent review, summarize, chat).
   * When {@code reviewMaxTokens} is unset (0), falls back to {@code maxTokens}.
   * @since 1.0.0
 */
  public int resolvedReviewMaxTokens() {
    return reviewMaxTokens > 0 ? reviewMaxTokens : maxTokens;
  }
}