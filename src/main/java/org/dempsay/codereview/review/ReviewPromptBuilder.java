package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.ingest.ChangedFile;

public final class ReviewPromptBuilder {

  private ReviewPromptBuilder() {
  }

  public static String build(final List<ChangedFile> changedFiles) {
    final StringBuilder prompt = new StringBuilder();
    prompt.append(
        "You are a code reviewer. Review the following git diff(s) and provide concise, actionable feedback."
    );
    prompt.append(System.lineSeparator()).append(System.lineSeparator());

    for (final ChangedFile file : changedFiles) {
      prompt.append("=== ").append(file.path()).append(" [").append(file.changeType()).append("] ===");
      prompt.append(System.lineSeparator());
      if (!file.included() || !file.hasDiff()) {
        prompt.append("(skipped: ").append(file.skipReason() == null ? "no diff" : file.skipReason()).append(")");
      } else {
        prompt.append(file.diff());
      }
      prompt.append(System.lineSeparator()).append(System.lineSeparator());
    }
    return prompt.toString();
  }
}