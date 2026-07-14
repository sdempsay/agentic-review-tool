package org.dempsay.codereview.review;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dempsay.codereview.ingest.ChangedFile;

/**
 * Validates agent review output against programmatic guardrails.
 *
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class ReviewOutputValidator {

  private static final Pattern FINDING_BULLET = Pattern.compile(
      "^\\s*- \\s*`?([^`:\\s]+:[^`\\s]+)`?\\s*—\\s*(must-fix|nit)\\s*—",
      Pattern.MULTILINE
  );
  private static final Pattern VERBOSE_CLEAN =
      Pattern.compile("(?i)Clean:\\s*all\\s+\\d+\\s+files");
  private static final Pattern RE_EVALUATION =
      Pattern.compile("(?i)(correction|re-evaluat|false positive)");
  private static final Pattern WILDCARD_IMPORT_CLAIM =
      Pattern.compile("(?i)(wildcard\\s+import|import\\s+\\S+\\.\\*|star\\s+import)");
  private static final Pattern MISSING_SPACE_BEFORE_PAREN =
      Pattern.compile("(?i)(missing|no|lack)\\s+space\\s+before\\s*\\(");
  private static final Pattern CONTROL_KEYWORD_WITH_SPACE =
      Pattern.compile("\\b(if|for|while)\\s*\\(");

  private static final Set<String> NIT_ONLY_RULESETS = Set.of("java-formatting", "java-javadoc");

  private ReviewOutputValidator() {
  }

  /**
   * Result of validating agent output.
   *
   * @param valid whether the output passed all checks
   * @param violations human-readable violation messages
   * @since 1.0.0
   */
  public record ValidationResult(boolean valid, List<String> violations) {

    /**
     * Formats violations for a retry prompt appendix.
     *
     * @return the summary
     * @since 1.0.0
     */
    public String violationSummary() {
      return violations.stream().map(violation -> "- " + violation).reduce((a, b) -> a + "\n" + b).orElse("");
    }
  }

  /**
   * Validates agent findings against scoped files and ruleset severity.
   *
   * @param agentName the agentName
   * @param scopedFiles the scopedFiles
   * @param output the output
   * @return the validation result
   * @since 1.0.0
   */
  public static ValidationResult validate(
      final String agentName,
      final List<ChangedFile> scopedFiles,
      final String output
  ) {
    if (output == null || output.isBlank()) {
      return new ValidationResult(true, List.of());
    }

    final List<String> violations = new ArrayList<>();
    final String trimmed = output.trim();
    final DiffLineIndex diffIndex = DiffLineIndex.fromFiles(scopedFiles);
    final Set<String> scopedPaths = scopedFiles.stream().map(ChangedFile::path).collect(Collectors.toSet());
    final String rulesetId = baseRulesetId(agentName);
    final boolean allowsMustFix = allowsMustFix(rulesetId);

    if (RE_EVALUATION.matcher(trimmed).find()) {
      violations.add("contains re-evaluation narrative (Correction, re-evaluat, false positive)");
    }

    final List<FindingBullet> bullets = parseFindingBullets(trimmed);
    if (!bullets.isEmpty() && VERBOSE_CLEAN.matcher(trimmed).find()) {
      violations.add("enumerates clean file count while findings are present");
    }

    for (final FindingBullet bullet : bullets) {
      validateBullet(bullet, scopedPaths, diffIndex, allowsMustFix, violations);
    }

    return new ValidationResult(violations.isEmpty(), List.copyOf(violations));
  }

  private static void validateBullet(
      final FindingBullet bullet,
      final Set<String> scopedPaths,
      final DiffLineIndex diffIndex,
      final boolean allowsMustFix,
      final List<String> violations
  ) {
    if (!scopedPaths.contains(bullet.path())) {
      violations.add("citation path out of scope: " + bullet.path());
      return;
    }

    if (!diffIndex.hasAddedLine(bullet.path(), bullet.lineNumber())) {
      violations.add("line " + bullet.path() + ":" + bullet.lineNumber() + " is not a + line in the diff");
    }

    if ("must-fix".equals(bullet.severity()) && !allowsMustFix) {
      violations.add("must-fix not allowed for ruleset on " + bullet.path() + ":" + bullet.lineNumber());
    }

    final String description = bullet.description().toLowerCase(Locale.ROOT);
    if (WILDCARD_IMPORT_CLAIM.matcher(description).find() && !diffIndex.hasWildcardImport(bullet.path())) {
      violations.add("wildcard-import claim without import ...* in diff for " + bullet.path());
    }

    if (MISSING_SPACE_BEFORE_PAREN.matcher(description).find()) {
      final String lineText = diffIndex.lineText(bullet.path(), bullet.lineNumber());
      if (CONTROL_KEYWORD_WITH_SPACE.matcher(lineText).find()) {
        violations.add("bogus spacing claim on line that already has keyword-space-paren: "
            + bullet.path() + ":" + bullet.lineNumber());
      }
    }
  }

  private static List<FindingBullet> parseFindingBullets(final String text) {
    final List<FindingBullet> bullets = new ArrayList<>();
    final Matcher matcher = FINDING_BULLET.matcher(text);
    while (matcher.find()) {
      final String citation = matcher.group(1);
      final int colon = citation.lastIndexOf(':');
      if (colon <= 0 || colon >= citation.length() - 1) {
        continue;
      }
      final String path = citation.substring(0, colon);
      final int lineNumber = Integer.parseInt(citation.substring(colon + 1));
      final String severity = matcher.group(2);
      final int descriptionEnd = text.indexOf('\n', matcher.end());
      final String description = (descriptionEnd < 0
          ? text.substring(matcher.end())
          : text.substring(matcher.end(), descriptionEnd)).trim();
      bullets.add(new FindingBullet(path, lineNumber, severity, description));
    }
    return bullets;
  }

  private static String baseRulesetId(final String agentName) {
    final int batchIndex = agentName.indexOf(" (batch ");
    if (batchIndex > 0) {
      return agentName.substring(0, batchIndex);
    }
    return agentName;
  }

  private static boolean allowsMustFix(final String rulesetId) {
    return !NIT_ONLY_RULESETS.contains(rulesetId);
  }

  private record FindingBullet(String path, int lineNumber, String severity, String description) {
  }
}
