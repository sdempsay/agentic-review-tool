package org.dempsay.codereview.review;

import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

/**
 * File and ruleset target for chat delegation.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record DelegationTarget(Rule rule, ChangedFile file, boolean generalFallback) {

  /**
   * Creates a delegation or review task for a ruleset.
   * 
   * @param rule the rule
   * @param file the file
   * @return the result
   * @since 1.0.0
 */
  public static DelegationTarget forRule(final Rule rule, final ChangedFile file) {
    return new DelegationTarget(rule, file, false);
  }

  /**
   * Creates a general-fallback delegation or review task.
   * 
   * @param file the file
   * @return the result
   * @since 1.0.0
 */
  public static DelegationTarget general(final ChangedFile file) {
    return new DelegationTarget(null, file, true);
  }

  /**
   * Returns the agent name for this task or delegation.
   * 
   * @return the result
   * @since 1.0.0
 */
  public String agentName() {
    return generalFallback ? "general" : rule.id();
  }
}