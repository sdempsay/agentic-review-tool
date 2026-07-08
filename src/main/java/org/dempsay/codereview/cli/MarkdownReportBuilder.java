package org.dempsay.codereview.cli;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

/**
 * Builds a Markdown report from review output.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class MarkdownReportBuilder {

  private MarkdownReportBuilder() {
  }

  /**
   * Builds a prompt or report from the given inputs.
   * 
   * @param scopeDescription the scopeDescription
   * @param changedFiles the changedFiles
   * @param classification the classification
   * @param reviewBody the reviewBody
   * @return the result
   * @since 1.0.0
 */
  public static String build(
      final String scopeDescription,
      final List<ChangedFile> changedFiles,
      final Map<String, List<Rule>> classification,
      final String reviewBody
  ) {
    final StringBuilder markdown = new StringBuilder();
    markdown.append("# Code Review Report").append(System.lineSeparator()).append(System.lineSeparator());
    markdown.append("**Generated:** ").append(Instant.now()).append(System.lineSeparator());
    markdown.append("**Scope:** ").append(scopeDescription).append(System.lineSeparator()).append(System.lineSeparator());
    appendIngestSection(markdown, changedFiles);
    appendClassificationSection(markdown, classification);
    if (reviewBody != null && !reviewBody.isBlank()) {
      appendReviewSection(markdown, reviewBody);
    }
    return markdown.toString();
  }

  private static void appendIngestSection(final StringBuilder markdown, final List<ChangedFile> changedFiles) {
    final long withDiff = changedFiles.stream().filter(ChangedFile::hasDiff).count();
    final long skipped = changedFiles.size() - withDiff;

    markdown.append("## Ingest").append(System.lineSeparator()).append(System.lineSeparator());
    markdown.append("- Files changed: ").append(changedFiles.size()).append(System.lineSeparator());
    markdown.append("- Reviewable diffs: ").append(withDiff).append(System.lineSeparator());
    markdown.append("- Skipped: ").append(skipped).append(System.lineSeparator()).append(System.lineSeparator());

    for (final ChangedFile file : changedFiles) {
      if (file.included()) {
        markdown.append("- `").append(file.path()).append("` [").append(file.changeType());
        markdown.append(", ").append(file.diff().length()).append(" bytes]").append(System.lineSeparator());
      } else {
        markdown.append("- `").append(file.path()).append("` [").append(file.changeType());
        markdown.append(", skipped: ").append(file.skipReason()).append("]").append(System.lineSeparator());
      }
    }
    markdown.append(System.lineSeparator());
  }

  private static void appendClassificationSection(
      final StringBuilder markdown,
      final Map<String, List<Rule>> classification
  ) {
    markdown.append("## Classification").append(System.lineSeparator()).append(System.lineSeparator());
    if (classification.isEmpty()) {
      markdown.append("No changed files detected.").append(System.lineSeparator()).append(System.lineSeparator());
      return;
    }

    for (final Map.Entry<String, List<Rule>> entry : classification.entrySet()) {
      markdown.append("### `").append(entry.getKey()).append("`").append(System.lineSeparator());
      final List<Rule> rules = entry.getValue();
      if (rules.isEmpty()) {
        markdown.append("- (no matching rules)").append(System.lineSeparator());
      } else {
        for (final Rule rule : rules) {
          markdown.append("- ").append(rule.id()).append(System.lineSeparator());
        }
      }
      markdown.append(System.lineSeparator());
    }
  }

  private static void appendReviewSection(final StringBuilder markdown, final String reviewBody) {
    markdown.append("## Review").append(System.lineSeparator()).append(System.lineSeparator());
    markdown.append(toMarkdownReview(reviewBody.trim())).append(System.lineSeparator());
  }

  static String toMarkdownReview(final String reviewBody) {
    return reviewBody
        .replace("--- Agent Reviews ---", "### Agent Reviews")
        .replace("--- Summary ---", "### Summary")
        .replaceAll("=== Review: (.+) ===", "### Review: $1");
  }
}