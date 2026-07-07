package org.dempsay.codereview.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RulesClassifier {

  private RulesClassifier() {
  }

  public static Map<String, List<Rule>> classify(final List<Rule> rules, final List<String> filePaths) {
    final Map<String, List<Rule>> matches = new LinkedHashMap<>();
    for (final String filePath : filePaths) {
      final List<Rule> matchedRules = new ArrayList<>();
      for (final Rule rule : rules) {
        if (matchesAnyGlob(rule.pathGlobs(), filePath)) {
          matchedRules.add(rule);
        }
      }
      matches.put(filePath, List.copyOf(matchedRules));
    }
    return matches;
  }

  public static List<Rule> classifyFile(final List<Rule> rules, final String filePath) {
    final List<Rule> matchedRules = new ArrayList<>();
    for (final Rule rule : rules) {
      if (matchesAnyGlob(rule.pathGlobs(), filePath)) {
        matchedRules.add(rule);
      }
    }
    return List.copyOf(matchedRules);
  }

  private static boolean matchesAnyGlob(final List<String> globs, final String filePath) {
    for (final String glob : globs) {
      if (PathGlobMatcher.matches(glob, filePath)) {
        return true;
      }
    }
    return false;
  }
}