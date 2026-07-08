package org.dempsay.codereview.review;

public final class AgentFindingsSanitizer {

  private AgentFindingsSanitizer() {
  }

  /**
   * Prepares agent output for the summarize stage. When an agent lists findings and later
   * retracts them (re-evaluation note, invalid markers) but ends with {@code ## Clean},
   * only the clean verdict is passed through.
   */
  public static String forSummarize(final String findings) {
    if (findings == null || findings.isBlank()) {
      return findings;
    }
    final String trimmed = findings.trim();
    if (endsWithCleanVerdict(trimmed) && containsRetraction(trimmed)) {
      return "## Clean";
    }
    return trimmed;
  }

  private static boolean endsWithCleanVerdict(final String text) {
    final String[] lines = text.split("\\R");
    for (int index = lines.length - 1; index >= 0; index--) {
      final String line = lines[index].trim();
      if (line.isEmpty()) {
        continue;
      }
      return "## Clean".equals(line);
    }
    return false;
  }

  private static boolean containsRetraction(final String text) {
    final String lower = text.toLowerCase();
    return lower.contains("invalid")
        || lower.contains("re-evaluat")
        || lower.contains("retract")
        || lower.contains("out of scope")
        || lower.contains("omit the finding");
  }
}