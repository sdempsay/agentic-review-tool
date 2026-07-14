package org.dempsay.codereview.review;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.dempsay.codereview.ingest.ChangedFile;

/**
 * Scans ingested diffs for exceptional-pattern violations on {@code +} lines in
 * {@code src/main/java}.
 *
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
final class ExceptionalDiffScanner {

  private static final Pattern MAIN_JAVA =
      Pattern.compile("^(.*/)?src/main/java/.+\\.java$");
  private static final Pattern THROWS_ON_PLUS = Pattern.compile("^\\+.*\\bthrows\\s+[\\w.]+");
  private static final Pattern TRY_ON_PLUS = Pattern.compile("^\\+\\s*try\\s*\\{");
  private static final Pattern CATCH_IO_ON_PLUS =
      Pattern.compile("^\\+.*\\bcatch\\s*\\(\\s*(?:final\\s+)?IOException\\b");
  private static final Pattern THROW_STATE_ON_PLUS =
      Pattern.compile("^\\+.*\\bthrow\\s+new\\s+IllegalStateException\\b");
  private static final Pattern HUNK_HEADER =
      Pattern.compile("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@");

  private ExceptionalDiffScanner() {
  }

  /**
   * Returns human-readable violations found on {@code +} lines in main sources.
   *
   * @param agentName the agentName
   * @param scopedFiles the scopedFiles
   * @return violation messages (empty when none)
   * @since 1.0.0
   */
  static List<String> scanMainJavaPlusLines(final String agentName, final List<ChangedFile> scopedFiles) {
    if (!isExceptionalAgent(agentName)) {
      return List.of();
    }
    final List<String> violations = new ArrayList<>();
    for (final ChangedFile file : scopedFiles) {
      if (!MAIN_JAVA.matcher(file.path()).matches() || !file.hasDiff()) {
        continue;
      }
      scanFile(file, violations);
    }
    return List.copyOf(violations);
  }

  private static boolean isExceptionalAgent(final String agentName) {
    final String base = agentName.contains(" (batch ")
        ? agentName.substring(0, agentName.indexOf(" (batch "))
        : agentName;
    return "java-exceptional".equals(base);
  }

  private static void scanFile(final ChangedFile file, final List<String> violations) {
    int newLine = 0;
    boolean inHunk = false;
    for (final String rawLine : file.diff().split("\\R", -1)) {
      final var header = HUNK_HEADER.matcher(rawLine);
      if (header.find()) {
        newLine = Integer.parseInt(header.group(1));
        inHunk = true;
        continue;
      }
      if (!inHunk) {
        continue;
      }
      if (rawLine.startsWith("+") && !rawLine.startsWith("+++")) {
        noteViolation(file.path(), newLine, rawLine, violations);
        newLine++;
        continue;
      }
      if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) {
        newLine++;
        continue;
      }
      if (rawLine.startsWith("-") && !rawLine.startsWith("---")) {
        continue;
      }
    }
  }

  private static void noteViolation(
      final String path,
      final int lineNumber,
      final String plusLine,
      final List<String> violations
  ) {
    if (THROWS_ON_PLUS.matcher(plusLine).find()) {
      violations.add(path + ":" + lineNumber + " — + line declares throws (adapter boundary / non-exceptional I/O)");
      return;
    }
    if (TRY_ON_PLUS.matcher(plusLine).find()) {
      violations.add(path + ":" + lineNumber + " — + line opens try block (prefer ExceptionalResource/Supplier over catch)");
      return;
    }
    if (CATCH_IO_ON_PLUS.matcher(plusLine).find()) {
      violations.add(path + ":" + lineNumber + " — + line catches IOException (prefer ExceptionalResource/Supplier)");
      return;
    }
    if (THROW_STATE_ON_PLUS.matcher(plusLine).find()) {
      violations.add(path + ":" + lineNumber + " — + line throws IllegalStateException for I/O setup (use ExceptionalSupplier)");
    }
  }
}