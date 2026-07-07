package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

public record RulesetReviewTask(
    Rule rule,
    List<ChangedFile> files,
    int batchIndex,
    int batchCount,
    boolean exceedsContextCap
) {

  public RulesetReviewTask {
    files = List.copyOf(files);
    if (batchIndex < 1 || batchCount < 1 || batchIndex > batchCount) {
      throw new IllegalArgumentException("Invalid batch coordinates: " + batchIndex + "/" + batchCount);
    }
  }

  public static RulesetReviewTask forRule(final Rule rule, final List<ChangedFile> files) {
    return forRule(rule, files, 1, 1);
  }

  public static RulesetReviewTask forRule(
      final Rule rule,
      final List<ChangedFile> files,
      final int batchIndex,
      final int batchCount
  ) {
    return forRule(rule, files, batchIndex, batchCount, false);
  }

  public static RulesetReviewTask forRule(
      final Rule rule,
      final List<ChangedFile> files,
      final int batchIndex,
      final int batchCount,
      final boolean exceedsContextCap
  ) {
    return new RulesetReviewTask(rule, files, batchIndex, batchCount, exceedsContextCap);
  }

  public static RulesetReviewTask generalFallback(final List<ChangedFile> files) {
    return generalFallback(files, 1, 1);
  }

  public static RulesetReviewTask generalFallback(
      final List<ChangedFile> files,
      final int batchIndex,
      final int batchCount
  ) {
    return generalFallback(files, batchIndex, batchCount, false);
  }

  public static RulesetReviewTask generalFallback(
      final List<ChangedFile> files,
      final int batchIndex,
      final int batchCount,
      final boolean exceedsContextCap
  ) {
    return new RulesetReviewTask(null, files, batchIndex, batchCount, exceedsContextCap);
  }

  public boolean isGeneralFallback() {
    return rule == null;
  }

  public String agentName() {
    final String base = rule == null ? "general" : rule.id();
    if (batchCount <= 1) {
      return base;
    }
    return base + " (batch " + batchIndex + "/" + batchCount + ")";
  }
}