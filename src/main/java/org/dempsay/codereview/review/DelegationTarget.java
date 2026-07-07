package org.dempsay.codereview.review;

import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

public record DelegationTarget(Rule rule, ChangedFile file, boolean generalFallback) {

  public static DelegationTarget forRule(final Rule rule, final ChangedFile file) {
    return new DelegationTarget(rule, file, false);
  }

  public static DelegationTarget general(final ChangedFile file) {
    return new DelegationTarget(null, file, true);
  }

  public String agentName() {
    return generalFallback ? "general" : rule.id();
  }
}