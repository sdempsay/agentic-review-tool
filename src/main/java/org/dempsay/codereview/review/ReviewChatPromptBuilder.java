package org.dempsay.codereview.review;

/**
 * Builds system prompts for follow-up chat.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class ReviewChatPromptBuilder {

  private ReviewChatPromptBuilder() {
  }

  /**
   * Builds the system prompt for follow-up chat.
   * 
   * @param reportText the reportText
   * @return the result
   * @since 1.0.0
 */
  public static String buildOrchestratorSystemPrompt(final String reportText) {
    return buildOrchestratorSystemPrompt(reportText, null);
  }

  /**
   * Builds the system prompt for follow-up chat.
   * 
   * @param reportText the reportText
   * @param guardrails the guardrails
   * @return the result
   * @since 1.0.0
 */
  public static String buildOrchestratorSystemPrompt(final String reportText, final String guardrails) {
    final StringBuilder prompt = new StringBuilder();
    prompt.append("You are a code review assistant. The user completed an automated review");
    prompt.append(" and may ask follow-up questions.");
    prompt.append(System.lineSeparator());
    prompt.append("Answer from the report below. If you cannot answer without seeing a specific");
    prompt.append(" file diff, say which file path the user should reference.");
    prompt.append(System.lineSeparator()).append(System.lineSeparator());
    if (guardrails != null && !guardrails.isBlank()) {
      prompt.append(guardrails.trim()).append(System.lineSeparator()).append(System.lineSeparator());
    }
    prompt.append("## Review Report").append(System.lineSeparator()).append(System.lineSeparator());
    prompt.append(reportText.trim());
    return prompt.toString();
  }
}