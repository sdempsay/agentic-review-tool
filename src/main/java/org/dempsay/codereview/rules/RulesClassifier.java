package org.dempsay.codereview.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Matches review rules to file paths.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
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

  /**
   * Classifies rules that match a single file path.
   * 
   * @param rules the rules
   * @param filePath the filePath
   * @return the result
   * @since 1.0.0
 */
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