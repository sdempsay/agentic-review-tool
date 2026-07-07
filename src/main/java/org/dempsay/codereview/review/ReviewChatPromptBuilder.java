package org.dempsay.codereview.review;

public final class ReviewChatPromptBuilder {

  private ReviewChatPromptBuilder() {
  }

  public static String buildOrchestratorSystemPrompt(final String reportText) {
    final StringBuilder prompt = new StringBuilder();
    prompt.append("You are a code review assistant. The user completed an automated review");
    prompt.append(" and may ask follow-up questions.");
    prompt.append(System.lineSeparator());
    prompt.append("Answer from the report below. If you cannot answer without seeing a specific");
    prompt.append(" file diff, say which file path the user should reference.");
    prompt.append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append("## Review Report").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append(reportText.trim());
    return prompt.toString();
  }
}