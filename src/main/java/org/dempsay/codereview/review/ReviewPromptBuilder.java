package org.dempsay.codereview.review;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

/**
 * Builds LLM prompts for agent and follow-up reviews.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class ReviewPromptBuilder {

  private ReviewPromptBuilder() {
  }

  /**
   * Builds a review prompt for a ruleset and files.
   * 
   * @param rule the rule
   * @param changedFiles the changedFiles
   * @return the result
   * @since 1.0.0
 */
  public static String buildForRuleset(final Rule rule, final List<ChangedFile> changedFiles) {
    return buildForRuleset(rule, changedFiles, ReviewContentMode.resolve(changedFiles));
  }

  /**
   * Builds a review prompt for a ruleset and files.
   * 
   * @param rule the rule
   * @param changedFiles the changedFiles
   * @param contentMode the contentMode
   * @return the result
   * @since 1.0.0
 */
  public static String buildForRuleset(
      final Rule rule,
      final List<ChangedFile> changedFiles,
      final ReviewContentMode contentMode
  ) {
    return buildForRuleset(rule, changedFiles, contentMode, (ReviewPromptSupplements) null);
  }

  /**
   * Builds a review prompt for a ruleset and files.
   * 
   * @param rule the rule
   * @param changedFiles the changedFiles
   * @param contentMode the contentMode
   * @param outputFormat the outputFormat
   * @return the result
   * @since 1.0.0
 */
  public static String buildForRuleset(
      final Rule rule,
      final List<ChangedFile> changedFiles,
      final ReviewContentMode contentMode,
      final String outputFormat
  ) {
    return buildForRuleset(rule, changedFiles, contentMode, supplementsFromOutputFormat(outputFormat));
  }

  /**
   * Builds a review prompt for a ruleset and files.
   * 
   * @param rule the rule
   * @param changedFiles the changedFiles
   * @param contentMode the contentMode
   * @param supplements the supplements
   * @return the result
   * @since 1.0.0
 */
  public static String buildForRuleset(
      final Rule rule,
      final List<ChangedFile> changedFiles,
      final ReviewContentMode contentMode,
      final ReviewPromptSupplements supplements
  ) {
    final StringBuilder prompt = new StringBuilder();
    prompt.append("You are a specialized code review agent for the \"").append(rule.id()).append("\" ruleset.");
    prompt.append(System.lineSeparator());
    if (contentMode == ReviewContentMode.FULL_FILE) {
      prompt.append("Review the full file contents below using the ruleset instructions.");
    } else {
      prompt.append(
          "Review only added/changed lines in the unified diffs below (see Diff review discipline)."
      );
    }
    prompt.append(System.lineSeparator()).append(System.lineSeparator());
    appendPromptSection(prompt, guardrailsFrom(supplements));
    prompt.append("## Ruleset Instructions").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append(rule.promptBody().trim()).append(System.lineSeparator()).append(System.lineSeparator());
    appendPromptSection(prompt, outputFormatFrom(supplements));
    appendFiles(prompt, Map.of(), changedFiles, contentMode);
    return prompt.toString();
  }

  /**
   * Builds a follow-up prompt for a ruleset and file.
   * 
   * @param rule the rule
   * @param file the file
   * @param question the question
   * @param reportText the reportText
   * @return the result
   * @since 1.0.0
 */
  public static String buildFollowUp(
      final Rule rule,
      final ChangedFile file,
      final String question,
      final String reportText
  ) {
    return buildFollowUp(rule, file, question, reportText, null);
  }

  /**
   * Builds a follow-up prompt for a ruleset and file.
   * 
   * @param rule the rule
   * @param file the file
   * @param question the question
   * @param reportText the reportText
   * @param supplements the supplements
   * @return the result
   * @since 1.0.0
 */
  public static String buildFollowUp(
      final Rule rule,
      final ChangedFile file,
      final String question,
      final String reportText,
      final ReviewPromptSupplements supplements
  ) {
    final ReviewContentMode contentMode = file.changeType() == ChangeType.EXISTING
        ? ReviewContentMode.FULL_FILE
        : ReviewContentMode.DIFF;
    final StringBuilder prompt = new StringBuilder();
    prompt.append("You are the \"").append(rule.id()).append("\" review agent.");
    prompt.append(System.lineSeparator());
    prompt.append("Answer the follow-up question using the ruleset instructions and the ")
        .append(fileContextLabel(contentMode))
        .append(".");
    prompt.append(System.lineSeparator()).append(System.lineSeparator());
    appendPromptSection(prompt, guardrailsFrom(supplements));
    prompt.append("## Ruleset Instructions").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append(rule.promptBody().trim()).append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append("## Prior Review Report").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append(reportText.trim()).append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append("## Follow-up Question").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append(question.trim()).append(System.lineSeparator()).append(System.lineSeparator());
    appendFiles(prompt, Map.of(), List.of(file), contentMode);
    return prompt.toString();
  }

  /**
   * Builds a general follow-up prompt for a file.
   * 
   * @param file the file
   * @param question the question
   * @param reportText the reportText
   * @return the result
   * @since 1.0.0
 */
  public static String buildGeneralFollowUp(
      final ChangedFile file,
      final String question,
      final String reportText
  ) {
    return buildGeneralFollowUp(file, question, reportText, null);
  }

  /**
   * Builds a general follow-up prompt for a file.
   * 
   * @param file the file
   * @param question the question
   * @param reportText the reportText
   * @param supplements the supplements
   * @return the result
   * @since 1.0.0
 */
  public static String buildGeneralFollowUp(
      final ChangedFile file,
      final String question,
      final String reportText,
      final ReviewPromptSupplements supplements
  ) {
    final ReviewContentMode contentMode = file.changeType() == ChangeType.EXISTING
        ? ReviewContentMode.FULL_FILE
        : ReviewContentMode.DIFF;
    final StringBuilder prompt = new StringBuilder();
    prompt.append("You are the general code review agent.");
    prompt.append(System.lineSeparator());
    prompt.append("Answer the follow-up question using the ")
        .append(fileContextLabel(contentMode))
        .append(" and prior review context.");
    prompt.append(System.lineSeparator()).append(System.lineSeparator());
    appendPromptSection(prompt, guardrailsFrom(supplements));
    prompt.append("## Prior Review Report").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append(reportText.trim()).append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append("## Follow-up Question").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append(question.trim()).append(System.lineSeparator()).append(System.lineSeparator());
    appendFiles(prompt, Map.of(), List.of(file), contentMode);
    return prompt.toString();
  }

  /**
   * Builds a general fallback review prompt.
   * 
   * @param changedFiles the changedFiles
   * @return the result
   * @since 1.0.0
 */
  public static String buildGeneralFallback(final List<ChangedFile> changedFiles) {
    return buildGeneralFallback(changedFiles, ReviewContentMode.resolve(changedFiles));
  }

  /**
   * Builds a general fallback review prompt.
   * 
   * @param changedFiles the changedFiles
   * @param contentMode the contentMode
   * @return the result
   * @since 1.0.0
 */
  public static String buildGeneralFallback(
      final List<ChangedFile> changedFiles,
      final ReviewContentMode contentMode
  ) {
    return buildGeneralFallback(changedFiles, contentMode, (ReviewPromptSupplements) null);
  }

  /**
   * Builds a general fallback review prompt.
   * 
   * @param changedFiles the changedFiles
   * @param contentMode the contentMode
   * @param outputFormat the outputFormat
   * @return the result
   * @since 1.0.0
 */
  public static String buildGeneralFallback(
      final List<ChangedFile> changedFiles,
      final ReviewContentMode contentMode,
      final String outputFormat
  ) {
    return buildGeneralFallback(changedFiles, contentMode, supplementsFromOutputFormat(outputFormat));
  }

  /**
   * Builds a general fallback review prompt.
   * 
   * @param changedFiles the changedFiles
   * @param contentMode the contentMode
   * @param supplements the supplements
   * @return the result
   * @since 1.0.0
 */
  public static String buildGeneralFallback(
      final List<ChangedFile> changedFiles,
      final ReviewContentMode contentMode,
      final ReviewPromptSupplements supplements
  ) {
    final StringBuilder prompt = new StringBuilder();
    prompt.append("You are a general code review agent.");
    prompt.append(System.lineSeparator());
    if (contentMode == ReviewContentMode.FULL_FILE) {
      prompt.append(
          "No specialized rules matched these files. Review the full file contents for correctness and clarity."
      );
    } else {
      prompt.append("No specialized rules matched these files. Review the diffs for correctness and clarity.");
    }
    prompt.append(System.lineSeparator()).append(System.lineSeparator());
    appendPromptSection(prompt, guardrailsFrom(supplements));
    appendPromptSection(prompt, outputFormatFrom(supplements));
    appendFiles(prompt, Map.of(), changedFiles, contentMode);
    return prompt.toString();
  }

  /**
   * Builds a prompt or report from the given inputs.
   * 
   * @param classification the classification
   * @param changedFiles the changedFiles
   * @return the result
   * @since 1.0.0
 */
  public static String build(final Map<String, List<Rule>> classification, final List<ChangedFile> changedFiles) {
    final ReviewContentMode contentMode = ReviewContentMode.resolve(changedFiles);
    final StringBuilder prompt = new StringBuilder();
    if (contentMode == ReviewContentMode.FULL_FILE) {
      prompt.append(
          "You are a code reviewer. Apply the review rules below when reviewing each file's full content."
      );
    } else {
      prompt.append(
          "You are a code reviewer. Apply the review rules below to added/changed lines in each "
              + "unified diff (see Diff review discipline)."
      );
    }
    prompt.append(System.lineSeparator()).append(System.lineSeparator());

    appendUniqueRules(prompt, classification, changedFiles);
    appendFiles(prompt, classification, changedFiles, contentMode);
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

  private static void appendFiles(
      final StringBuilder prompt,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles,
      final ReviewContentMode contentMode
  ) {
    final String sectionTitle = contentMode == ReviewContentMode.FULL_FILE
        ? "## Repository Files"
        : "## Changed Files";
    prompt.append(sectionTitle).append(System.lineSeparator()).append(System.lineSeparator());

    for (final ChangedFile file : changedFiles) {
      prompt.append("=== ").append(file.path()).append(" [").append(file.changeType()).append("] ===");
      prompt.append(System.lineSeparator());

      if (file.included() && file.hasDiff()) {
        appendApplicableRules(prompt, classification.getOrDefault(file.path(), List.of()));
        if (contentMode == ReviewContentMode.FULL_FILE) {
          prompt.append("```").append(System.lineSeparator());
          prompt.append(file.diff());
          if (!file.diff().endsWith(System.lineSeparator())) {
            prompt.append(System.lineSeparator());
          }
          prompt.append("```");
        } else {
          prompt.append("```diff").append(System.lineSeparator());
          prompt.append(file.diff());
          if (!file.diff().endsWith(System.lineSeparator())) {
            prompt.append(System.lineSeparator());
          }
          prompt.append("```");
        }
      } else {
        prompt.append("(skipped: ").append(file.skipReason() == null ? "no content" : file.skipReason()).append(")");
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

  private static String fileContextLabel(final ReviewContentMode contentMode) {
    return contentMode == ReviewContentMode.FULL_FILE ? "file content" : "file diff";
  }

  private static void appendPromptSection(final StringBuilder prompt, final String section) {
    if (section == null || section.isBlank()) {
      return;
    }
    prompt.append(section.trim()).append(System.lineSeparator()).append(System.lineSeparator());
  }

  private static ReviewPromptSupplements supplementsFromOutputFormat(final String outputFormat) {
    if (outputFormat == null) {
      return null;
    }
    return new ReviewPromptSupplements("", outputFormat);
  }

  private static String guardrailsFrom(final ReviewPromptSupplements supplements) {
    return supplements == null ? null : supplements.guardrails();
  }

  private static String outputFormatFrom(final ReviewPromptSupplements supplements) {
    return supplements == null ? null : supplements.outputFormat();
  }
}