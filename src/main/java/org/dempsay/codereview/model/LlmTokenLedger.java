package org.dempsay.codereview.model;

import dev.langchain4j.model.output.TokenUsage;
import java.util.ArrayList;
import java.util.List;
import org.dempsay.codereview.config.ModelConfig;

public final class LlmTokenLedger {

  private final List<CallUsage> calls = new ArrayList<>();
  private TokenUsage total = new TokenUsage(0, 0, 0);

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

  public boolean isEmpty() {
    return calls.isEmpty();
  }

  public List<CallUsage> calls() {
    return List.copyOf(calls);
  }

  public TokenUsage total() {
    return total;
  }

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

  public record CallUsage(String label, int inputTokens, int outputTokens, int totalTokens) {
  }
}