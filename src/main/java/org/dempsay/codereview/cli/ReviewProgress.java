package org.dempsay.codereview.cli;

import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import org.dempsay.codereview.config.ModelConfig;
import org.dempsay.codereview.model.LlmTokenLedger;
public final class ReviewProgress {

  private final CliVerbosity verbosity;
  private final LlmTokenLedger tokenLedger = new LlmTokenLedger();

  public ReviewProgress(final CliVerbosity verbosity) {
    this.verbosity = verbosity;
  }

  public static ReviewProgress create(final CliVerbosity verbosity) {
    return new ReviewProgress(verbosity);
  }

  public CliVerbosity verbosity() {
    return verbosity;
  }

  public boolean isQuiet() {
    return verbosity == CliVerbosity.QUIET;
  }

  public boolean shouldStreamLlm() {
    return verbosity != CliVerbosity.QUIET;
  }

  public boolean isVerbose() {
    return verbosity == CliVerbosity.VERBOSE;
  }

  public void stageStart(final String stage) {
    if (isQuiet()) {
      return;
    }
    log(String.format("[Pipeline] %s ...", stage));
  }

  public void stageComplete(final String stage, final long startedAtMs) {
    if (isQuiet()) {
      return;
    }
    log(String.format("[Pipeline] %s complete (%.1fs)", stage, elapsedSeconds(startedAtMs)));
  }

  public void agentStart(final String agentName, final int fileCount, final List<String> filePaths) {
    if (isQuiet()) {
      return;
    }
    log(String.format("[Review] %s (%d file%s)", agentName, fileCount, fileCount == 1 ? "" : "s"));
    if (isVerbose()) {
      for (final String filePath : filePaths) {
        log(String.format("  - %s", filePath));
      }
    }
  }

  public void batchCapResolved(final int contextTokens, final int softDiffKb) {
    if (isQuiet()) {
      return;
    }
    log(String.format(
        "[Pipeline] Ollama num_ctx=%d tokens; soft batch target=%d KB diffs",
        contextTokens,
        softDiffKb
    ));
  }

  public void batchCapFallback(final int softDiffKb) {
    if (isQuiet()) {
      return;
    }
    if (softDiffKb > 0) {
      log(String.format(
          "[Pipeline] Ollama num_ctx unavailable; using %d KB as soft and hard diff cap",
          softDiffKb
      ));
    }
  }

  public void agentContextWarning(final String agentName) {
    if (isQuiet()) {
      return;
    }
    log(String.format(
        "[Review] %s — batch may exceed model context; review may truncate",
        agentName
    ));
  }

  public void agentComplete(final String agentName, final long startedAtMs) {
    if (isQuiet()) {
      return;
    }
    log(String.format("[Review] %s complete (%.1fs)", agentName, elapsedSeconds(startedAtMs)));
  }

  public void llmStarted(final String label) {
    if (isQuiet()) {
      return;
    }
    log(String.format("[LLM] %s — waiting for response...", label));
  }

  public void streamToken(final String token) {
    if (!shouldStreamLlm() || token == null || token.isEmpty()) {
      return;
    }
    System.err.print(token);
  }

  public void streamThinking(final String thinking) {
    if (!isVerbose() || thinking == null || thinking.isEmpty()) {
      return;
    }
    System.err.print(thinking);
  }

  public void streamFinished() {
    if (!shouldStreamLlm()) {
      return;
    }
    System.err.println();
  }

  public LlmTokenLedger tokenLedger() {
    return tokenLedger;
  }

  public void recordTokenUsage(final String label, final TokenUsage usage) {
    tokenLedger.record(label, usage);
    if (!isVerbose() || tokenLedger.calls().isEmpty()) {
      return;
    }
    final LlmTokenLedger.CallUsage last = tokenLedger.calls().get(tokenLedger.calls().size() - 1);
    log(String.format(
        "[Tokens] %s: %d in / %d out / %d total",
        last.label(),
        last.inputTokens(),
        last.outputTokens(),
        last.totalTokens()
    ));
  }

  public void printTokenSummary(final ModelConfig model) {
    if (isQuiet()) {
      return;
    }
    log(tokenLedger.formatProgressSummary(model));
  }

  private static void log(final String message) {
    System.err.println(message);
  }

  private static double elapsedSeconds(final long startedAtMs) {
    return (System.currentTimeMillis() - startedAtMs) / 1000.0;
  }
}