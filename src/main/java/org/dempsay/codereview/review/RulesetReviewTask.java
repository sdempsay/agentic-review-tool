package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

/**
 * A single agent review task for a ruleset batch.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record RulesetReviewTask(
    Rule rule,
    List<ChangedFile> files,
    int batchIndex,
    int batchCount,
    boolean exceedsContextCap
) {

  /**
   * Creates a new RulesetReviewTask.
   * 
   * @since 1.0.0
 */
  public RulesetReviewTask {
    files = List.copyOf(files);
    if (batchIndex < 1 || batchCount < 1 || batchIndex > batchCount) {
      throw new IllegalArgumentException("Invalid batch coordinates: " + batchIndex + "/" + batchCount);
    }
  }

  /**
   * Creates a delegation or review task for a ruleset.
   * 
   * @param rule the rule
   * @param files the files
   * @return the result
   * @since 1.0.0
 */
  public static RulesetReviewTask forRule(final Rule rule, final List<ChangedFile> files) {
    return forRule(rule, files, 1, 1);
  }

  /**
   * Creates a delegation or review task for a ruleset.
   * 
   * @param rule the rule
   * @param files the files
   * @param batchIndex the batchIndex
   * @param batchCount the batchCount
   * @return the result
   * @since 1.0.0
 */
  public static RulesetReviewTask forRule(
      final Rule rule,
      final List<ChangedFile> files,
      final int batchIndex,
      final int batchCount
  ) {
    return forRule(rule, files, batchIndex, batchCount, false);
  }

  /**
   * Creates a delegation or review task for a ruleset.
   * 
   * @param rule the rule
   * @param files the files
   * @param batchIndex the batchIndex
   * @param batchCount the batchCount
   * @param exceedsContextCap the exceedsContextCap
   * @return the result
   * @since 1.0.0
 */
  public static RulesetReviewTask forRule(
      final Rule rule,
      final List<ChangedFile> files,
      final int batchIndex,
      final int batchCount,
      final boolean exceedsContextCap
  ) {
    return new RulesetReviewTask(rule, files, batchIndex, batchCount, exceedsContextCap);
  }

  /**
   * Creates a general-fallback review task.
   * 
   * @param files the files
   * @return the result
   * @since 1.0.0
 */
  public static RulesetReviewTask generalFallback(final List<ChangedFile> files) {
    return generalFallback(files, 1, 1);
  }

  /**
   * Creates a general-fallback review task.
   * 
   * @param files the files
   * @param batchIndex the batchIndex
   * @param batchCount the batchCount
   * @return the result
   * @since 1.0.0
 */
  public static RulesetReviewTask generalFallback(
      final List<ChangedFile> files,
      final int batchIndex,
      final int batchCount
  ) {
    return generalFallback(files, batchIndex, batchCount, false);
  }

  /**
   * Creates a general-fallback review task.
   * 
   * @param files the files
   * @param batchIndex the batchIndex
   * @param batchCount the batchCount
   * @param exceedsContextCap the exceedsContextCap
   * @return the result
   * @since 1.0.0
 */
  public static RulesetReviewTask generalFallback(
      final List<ChangedFile> files,
      final int batchIndex,
      final int batchCount,
      final boolean exceedsContextCap
  ) {
    return new RulesetReviewTask(null, files, batchIndex, batchCount, exceedsContextCap);
  }

  /**
   * Returns whether this task uses the general fallback agent.
   * 
   * @return the result
   * @since 1.0.0
 */
  public boolean isGeneralFallback() {
    return rule == null;
  }

  /**
   * Returns the agent name for this task or delegation.
   * 
   * @return the result
   * @since 1.0.0
 */
  public String agentName() {
    final String base = rule == null ? "general" : rule.id();
    if (batchCount <= 1) {
      return base;
    }
    return base + " (batch " + batchIndex + "/" + batchCount + ")";
  }
}