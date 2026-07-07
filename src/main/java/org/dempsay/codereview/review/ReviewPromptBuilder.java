package org.dempsay.codereview.review;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

public final class ReviewPromptBuilder {

  private ReviewPromptBuilder() {
  }

  public static String buildForRuleset(final Rule rule, final List<ChangedFile> changedFiles) {
    final StringBuilder prompt = new StringBuilder();
    prompt.append("You are a specialized code review agent for the \"").append(rule.id()).append("\" ruleset.");
    prompt.append(System.lineSeparator());
    prompt.append("Review only the files below using the ruleset instructions.");
    prompt.append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append("## Ruleset Instructions").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append(rule.promptBody().trim()).append(System.lineSeparator()).append(System.lineSeparator());
    appendChangedFiles(prompt, Map.of(), changedFiles);
    return prompt.toString();
  }

  public static String buildGeneralFallback(final List<ChangedFile> changedFiles) {
    final StringBuilder prompt = new StringBuilder();
    prompt.append("You are a general code review agent.");
    prompt.append(System.lineSeparator());
    prompt.append("No specialized rules matched these files. Review the diffs for correctness and clarity.");
    prompt.append(System.lineSeparator()).append(System.lineSeparator());
    appendChangedFiles(prompt, Map.of(), changedFiles);
    return prompt.toString();
  }

  public static String build(final Map<String, List<Rule>> classification, final List<ChangedFile> changedFiles) {
    final StringBuilder prompt = new StringBuilder();
    prompt.append(
        "You are a code reviewer. Apply the review rules below when reviewing each file's diff."
    );
    prompt.append(System.lineSeparator()).append(System.lineSeparator());

    appendUniqueRules(prompt, classification, changedFiles);
    appendChangedFiles(prompt, classification, changedFiles);
    return prompt.toString();
  }

  private static void appendUniqueRules(
      final StringBuilder prompt,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles
  ) {
    final Map<String, Rule> uniqueRules = collectUniqueRules(classification, changedFiles);
    if (uniqueRules.isEmpty()) {
      return;
    }

    prompt.append("## Review Rules").append(System.lineSeparator()).append(System.lineSeparator());
    for (final Rule rule : uniqueRules.values()) {
      prompt.append("### Rule: ").append(rule.id()).append(System.lineSeparator());
      prompt.append(rule.promptBody().trim()).append(System.lineSeparator()).append(System.lineSeparator());
    }
  }

  private static Map<String, Rule> collectUniqueRules(
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles
  ) {
    final Map<String, Rule> uniqueRules = new LinkedHashMap<>();
    for (final ChangedFile file : changedFiles) {
      if (!file.included() || !file.hasDiff()) {
        continue;
      }
      for (final Rule rule : classification.getOrDefault(file.path(), List.of())) {
        uniqueRules.putIfAbsent(rule.id(), rule);
      }
    }
    return uniqueRules;
  }

  private static void appendChangedFiles(
      final StringBuilder prompt,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles
  ) {
    prompt.append("## Changed Files").append(System.lineSeparator()).append(System.lineSeparator());

    for (final ChangedFile file : changedFiles) {
      prompt.append("=== ").append(file.path()).append(" [").append(file.changeType()).append("] ===");
      prompt.append(System.lineSeparator());

      if (file.included() && file.hasDiff()) {
        appendApplicableRules(prompt, classification.getOrDefault(file.path(), List.of()));
        prompt.append(file.diff());
      } else {
        prompt.append("(skipped: ").append(file.skipReason() == null ? "no diff" : file.skipReason()).append(")");
      }
      prompt.append(System.lineSeparator()).append(System.lineSeparator());
    }
  }

  private static void appendApplicableRules(final StringBuilder prompt, final List<Rule> matchedRules) {
    if (matchedRules.isEmpty()) {
      return;
    }
    final String ruleIds = matchedRules.stream().map(Rule::id).collect(Collectors.joining(", "));
    prompt.append("Applicable rules: ").append(ruleIds).append(System.lineSeparator());
  }
}