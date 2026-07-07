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
    final StringBuilder prompt = new StringBuilder();
    prompt.append(
        "You are a code review summarizer. Synthesize the specialized agent findings below into an"
    );
    prompt.append(" overall assessment.");
    prompt.append(System.lineSeparator()).append(System.lineSeparator());

    appendChangeStats(prompt, changedFiles, contentMode);
    appendAgentFindings(prompt, agentResults);
    appendOutputFormat(prompt);
    return prompt.toString();
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

  private static void appendAgentFindings(final StringBuilder prompt, final List<ReviewResult> agentResults) {
    prompt.append("## Agent Findings").append(System.lineSeparator()).append(System.lineSeparator());
    for (final ReviewResult result : agentResults) {
      prompt.append("### ").append(result.agentName()).append(System.lineSeparator());
      prompt.append(result.findings().trim()).append(System.lineSeparator()).append(System.lineSeparator());
    }
  }

  private static void appendOutputFormat(final StringBuilder prompt) {
    prompt.append("## Required Response Format").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append("Respond using exactly these markdown sections:").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append("### Health Score").append(System.lineSeparator());
    prompt.append("A single line: N/10 (1 = critical issues, 10 = excellent).").append(System.lineSeparator());
    prompt.append(System.lineSeparator());
    prompt.append("### Recommendation").append(System.lineSeparator());
    prompt.append("A single line, one of: APPROVE, APPROVE_WITH_NITS, REQUEST_CHANGES, BLOCK.");
    prompt.append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append("### Summary").append(System.lineSeparator());
    prompt.append("2-4 sentences on overall change health.").append(System.lineSeparator());
    prompt.append(System.lineSeparator());
    prompt.append("### Top Actions").append(System.lineSeparator());
    prompt.append("Bulleted list of up to 5 highest-priority fixes.");
  }
}