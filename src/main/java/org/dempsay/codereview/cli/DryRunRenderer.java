package org.dempsay.codereview.cli;

import java.util.List;
import java.util.Map;
import org.dempsay.codereview.rules.Rule;

public final class DryRunRenderer {

  private DryRunRenderer() {
  }

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