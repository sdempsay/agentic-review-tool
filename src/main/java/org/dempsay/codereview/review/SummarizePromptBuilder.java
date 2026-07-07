package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.ingest.ChangedFile;

public final class SummarizePromptBuilder {

  private SummarizePromptBuilder() {
  }

  public static String build(final List<ReviewResult> agentResults, final List<ChangedFile> changedFiles) {
    return build(agentResults, changedFiles, ReviewContentMode.resolve(changedFiles));
  }

  public static String build(
      final List<ReviewResult> agentResults,
      final List<ChangedFile> changedFiles,
      final ReviewContentMode contentMode
  ) {
    return build(agentResults, changedFiles, contentMode, null);
  }

  public static String build(
      final List<ReviewResult> agentResults,
      final List<ChangedFile> changedFiles,
      final ReviewContentMode contentMode,
      final String guardrails
  ) {
    final StringBuilder prompt = new StringBuilder();
    appendIntro(prompt, contentMode);
    appendGuardrails(prompt, guardrails);
    appendChangeStats(prompt, changedFiles, contentMode);
    if (contentMode == ReviewContentMode.FULL_FILE) {
      appendRepositoryCoverage(prompt, changedFiles);
    }
    appendAgentFindings(prompt, agentResults);
    appendOutputFormat(prompt, contentMode);
    return prompt.toString();
  }

  private static void appendIntro(final StringBuilder prompt, final ReviewContentMode contentMode) {
    if (contentMode == ReviewContentMode.FULL_FILE) {
      prompt.append(
          "You are a repository review summarizer. Synthesize the specialized agent findings below"
      );
      prompt.append(" into a codebase-level assessment.");
    } else {
      prompt.append(
          "You are a code review summarizer. Synthesize the specialized agent findings below into an"
      );
      prompt.append(" overall assessment.");
    }
    prompt.append(System.lineSeparator()).append(System.lineSeparator());
  }

  private static void appendChangeStats(
      final StringBuilder prompt,
      final List<ChangedFile> changedFiles,
      final ReviewContentMode contentMode
  ) {
    final long reviewable = changedFiles.stream().filter(ChangedFile::hasDiff).count();
    final long skipped = changedFiles.size() - reviewable;

    prompt.append("## Change Stats").append(System.lineSeparator());
    if (contentMode == ReviewContentMode.FULL_FILE) {
      prompt.append("- Files in scope: ").append(changedFiles.size()).append(System.lineSeparator());
      prompt.append("- Reviewable files: ").append(reviewable).append(System.lineSeparator());
    } else {
      prompt.append("- Files changed: ").append(changedFiles.size()).append(System.lineSeparator());
      prompt.append("- Reviewable diffs: ").append(reviewable).append(System.lineSeparator());
    }
    prompt.append("- Skipped: ").append(skipped).append(System.lineSeparator()).append(System.lineSeparator());
  }

  private static void appendRepositoryCoverage(final StringBuilder prompt, final List<ChangedFile> changedFiles) {
    final List<RepoHotspotAnalyzer.HotspotArea> hotspots = RepoHotspotAnalyzer.topAreas(changedFiles);
    prompt.append("## Repository Coverage").append(System.lineSeparator());
    if (hotspots.isEmpty()) {
      prompt.append("- No reviewable files in scope.").append(System.lineSeparator());
    } else {
      for (final RepoHotspotAnalyzer.HotspotArea hotspot : hotspots) {
        prompt.append("- `").append(hotspot.path()).append("`: ");
        prompt.append(hotspot.reviewableFiles()).append(" reviewable file");
        if (hotspot.reviewableFiles() != 1) {
          prompt.append('s');
        }
        prompt.append(System.lineSeparator());
      }
    }
    prompt.append(System.lineSeparator());
  }

  private static void appendAgentFindings(final StringBuilder prompt, final List<ReviewResult> agentResults) {
    prompt.append("## Agent Findings").append(System.lineSeparator()).append(System.lineSeparator());
    for (final ReviewResult result : agentResults) {
      prompt.append("### ").append(result.agentName()).append(System.lineSeparator());
      prompt.append(result.findings().trim()).append(System.lineSeparator()).append(System.lineSeparator());
    }
  }

  private static void appendOutputFormat(final StringBuilder prompt, final ReviewContentMode contentMode) {
    prompt.append("## Required Response Format").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append("Respond using exactly these markdown sections:").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append("### Health Score").append(System.lineSeparator());
    prompt.append("A single line: N/10 (1 = critical issues, 10 = excellent).").append(System.lineSeparator());
    prompt.append(System.lineSeparator());
    prompt.append("### Recommendation").append(System.lineSeparator());
    prompt.append("A single line, one of: APPROVE, APPROVE_WITH_NITS, REQUEST_CHANGES, BLOCK.");
    prompt.append(System.lineSeparator()).append(System.lineSeparator());

    if (contentMode == ReviewContentMode.FULL_FILE) {
      prompt.append("### Hotspot Areas").append(System.lineSeparator());
      prompt.append(
          "Bulleted list of 2-5 directories or modules with the highest concentration of findings"
      );
      prompt.append(" or risk. Reference concrete paths from the coverage stats and agent output.");
      prompt.append(System.lineSeparator()).append(System.lineSeparator());
      prompt.append("### Cross-Cutting Findings").append(System.lineSeparator());
      prompt.append(
          "Bulleted list of themes that recur across multiple files, packages, or agents"
      );
      prompt.append(" (for example repeated error-handling gaps or inconsistent patterns).");
      prompt.append(System.lineSeparator()).append(System.lineSeparator());
      prompt.append("### Summary").append(System.lineSeparator());
      prompt.append("2-4 sentences on overall codebase health and maintainability.").append(System.lineSeparator());
    } else {
      prompt.append("### Summary").append(System.lineSeparator());
      prompt.append("2-4 sentences on overall change health.").append(System.lineSeparator());
    }

    prompt.append(System.lineSeparator());
    prompt.append("### Top Actions").append(System.lineSeparator());
    prompt.append("Bulleted list of up to 5 highest-priority fixes.");
  }

  private static void appendGuardrails(final StringBuilder prompt, final String guardrails) {
    if (guardrails == null || guardrails.isBlank()) {
      return;
    }
    prompt.append(guardrails.trim()).append(System.lineSeparator()).append(System.lineSeparator());
  }
}