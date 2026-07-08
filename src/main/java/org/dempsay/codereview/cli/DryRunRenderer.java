package org.dempsay.codereview.cli;

import java.util.List;
import java.util.Map;
import org.dempsay.codereview.rules.Rule;

/**
 * Prints rule classification without calling the LLM.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class DryRunRenderer {

  private DryRunRenderer() {
  }

  /**
   * Renders output to stdout.
   * 
   * @param matches the matches
   * @since 1.0.0
 */
  public static void render(final Map<String, List<Rule>> matches) {
    if (matches.isEmpty()) {
      System.out.println("No changed files detected.");
      return;
    }

    for (final Map.Entry<String, List<Rule>> entry : matches.entrySet()) {
      System.out.println(entry.getKey() + ":");
      final List<Rule> rules = entry.getValue();
      if (rules.isEmpty()) {
        System.out.println("  (no matching rules)");
      } else {
        for (final Rule rule : rules) {
          System.out.println("  - " + rule.id());
        }
      }
    }
  }
}