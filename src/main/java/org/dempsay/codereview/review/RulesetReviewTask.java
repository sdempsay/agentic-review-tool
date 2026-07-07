package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

public record RulesetReviewTask(Rule rule, List<ChangedFile> files) {

  public RulesetReviewTask {
    files = List.copyOf(files);
  }

  public static RulesetReviewTask forRule(final Rule rule, final List<ChangedFile> files) {
    return new RulesetReviewTask(rule, files);
  }

  public static RulesetReviewTask generalFallback(final List<ChangedFile> files) {
    return new RulesetReviewTask(null, files);
  }

  public boolean isGeneralFallback() {
    return rule == null;
  }

  public String agentName() {
    return rule == null ? "general" : rule.id();
  }
}