package org.dempsay.codereview.model;

import dev.langchain4j.model.output.TokenUsage;
import java.util.ArrayList;
import java.util.List;
import org.dempsay.codereview.config.ModelConfig;

/**
 * Accumulates per-call and total token usage during a review.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class LlmTokenLedger {

  private final List<CallUsage> calls = new ArrayList<>();
  private TokenUsage total = new TokenUsage(0, 0, 0);

  /**
   * Records token usage for a labeled LLM call.
   * 
   * @param label the label
   * @param usage the usage
   * @since 1.0.0
 */
  public void record(final String label, final TokenUsage usage) {
    final TokenUsage normalized = LlmTokenUsageExtractor.normalize(usage);
    if (!LlmTokenUsageExtractor.hasUsage(normalized)) {
      return;
    }
    calls.add(new CallUsage(
        label,
        LlmTokenUsageExtractor.orZero(normalized.inputTokenCount()),
        LlmTokenUsageExtractor.orZero(normalized.outputTokenCount()),
        LlmTokenUsageExtractor.orZero(normalized.totalTokenCount())
    ));
    total = total.add(normalized);
  }

  /**
   * Returns whether any LLM calls were recorded.
   * 
   * @return the result
   * @since 1.0.0
 */
  public boolean isEmpty() {
    return calls.isEmpty();
  }

  /**
   * Returns recorded per-call token usage.
   * 
   * @return the result
   * @since 1.0.0
 */
  public List<CallUsage> calls() {
    return List.copyOf(calls);
  }

  /**
   * Returns cumulative token usage.
   * 
   * @return the result
   * @since 1.0.0
 */
  public TokenUsage total() {
    return total;
  }

  /**
   * Formats token usage for the review report.
   * 
   * @param model the model
   * @return the result
   * @since 1.0.0
 */
  public String formatReportSection(final ModelConfig model) {
    if (isEmpty()) {
      return "";
    }

    final StringBuilder section = new StringBuilder();
    section.append("--- Token Usage ---").append(System.lineSeparator());
    section.append("Provider: ").append(model.provider()).append(System.lineSeparator());
    section.append("Model: ").append(model.name()).append(System.lineSeparator());
    section.append(formatTotals(total)).append(System.lineSeparator());
    section.append(System.lineSeparator());
    section.append("Per call:").append(System.lineSeparator());
    for (final CallUsage call : calls) {
      section.append("- ").append(call.label()).append(": ");
      section.append(call.inputTokens()).append(" in / ");
      section.append(call.outputTokens()).append(" out / ");
      section.append(call.totalTokens()).append(" total");
      section.append(System.lineSeparator());
    }
    return section.toString().trim();
  }

  /**
   * Formats token usage for progress output.
   * 
   * @param model the model
   * @return the result
   * @since 1.0.0
 */
  public String formatProgressSummary(final ModelConfig model) {
    if (isEmpty()) {
      return "[Tokens] No usage reported by provider";
    }
    return String.format(
        "[Tokens] %s (%s) — %s across %d LLM call(s)",
        model.provider(),
        model.name(),
        formatTotals(total),
        calls.size()
    );
  }

  private static String formatTotals(final TokenUsage usage) {
    return LlmTokenUsageExtractor.orZero(usage.inputTokenCount())
        + " input, "
        + LlmTokenUsageExtractor.orZero(usage.outputTokenCount())
        + " output, "
        + LlmTokenUsageExtractor.orZero(usage.totalTokenCount())
        + " total";
  }

  /**
   * Token usage for a single LLM call.
   *
   * @param label call label shown in reports
   * @param inputTokens input tokens consumed
   * @param outputTokens output tokens generated
   * @param totalTokens total tokens reported by the provider
   * @since 1.0.0
   * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
   */
  public record CallUsage(String label, int inputTokens, int outputTokens, int totalTokens) {
  }
}