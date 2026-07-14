package org.dempsay.codereview.cli;

import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import org.dempsay.codereview.config.ModelConfig;
import org.dempsay.codereview.model.LlmTokenLedger;
/**
 * Progress and token-usage reporting during pipeline execution.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class ReviewProgress {

  private final CliVerbosity verbosity;
  private final LlmTokenLedger tokenLedger = new LlmTokenLedger();

  /**
   * Creates a new ReviewProgress.
   * 
   * @param verbosity the verbosity
   * @since 1.0.0
 */
  public ReviewProgress(final CliVerbosity verbosity) {
    this.verbosity = verbosity;
  }

  /**
   * Creates a progress reporter for the given verbosity.
   * 
   * @param verbosity the verbosity
   * @return the result
   * @since 1.0.0
 */
  public static ReviewProgress create(final CliVerbosity verbosity) {
    return new ReviewProgress(verbosity);
  }

  /**
   * Returns the configured verbosity level.
   * 
   * @return the result
   * @since 1.0.0
 */
  public CliVerbosity verbosity() {
    return verbosity;
  }

  /**
   * Returns whether progress output is suppressed.
   * 
   * @return the result
   * @since 1.0.0
 */
  public boolean isQuiet() {
    return verbosity == CliVerbosity.QUIET;
  }

  /**
   * Returns whether LLM output should be streamed.
   * 
   * @return the result
   * @since 1.0.0
 */
  public boolean shouldStreamLlm() {
    return verbosity != CliVerbosity.QUIET;
  }

  /**
   * Returns whether verbose progress is enabled.
   * 
   * @return the result
   * @since 1.0.0
 */
  public boolean isVerbose() {
    return verbosity == CliVerbosity.VERBOSE;
  }

  /**
   * Logs the start of a pipeline stage.
   * 
   * @param stage the stage
   * @since 1.0.0
 */
  public void stageStart(final String stage) {
    if (isQuiet()) {
      return;
    }
    log(String.format("[Pipeline] %s ...", stage));
  }

  /**
   * Logs completion of a pipeline stage.
   * 
   * @param stage the stage
   * @param startedAtMs the startedAtMs
   * @since 1.0.0
 */
  public void stageComplete(final String stage, final long startedAtMs) {
    if (isQuiet()) {
      return;
    }
    log(String.format("[Pipeline] %s complete (%.1fs)", stage, elapsedSeconds(startedAtMs)));
  }

  /**
   * Logs the start of an agent review.
   * 
   * @param agentName the agentName
   * @param fileCount the fileCount
   * @param filePaths the filePaths
   * @since 1.0.0
 */
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

  /**
   * Logs resolved Ollama context and soft batch cap.
   * 
   * @param contextTokens the contextTokens
   * @param softDiffKb the softDiffKb
   * @since 1.0.0
 */
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

  /**
   * Logs fallback when Ollama context is unavailable.
   * 
   * @param softDiffKb the softDiffKb
   * @since 1.0.0
 */
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

  /**
   * Logs a warning when a batch may exceed context.
   * 
   * @param agentName the agentName
   * @since 1.0.0
 */
  public void agentContextWarning(final String agentName) {
    if (isQuiet()) {
      return;
    }
    log(String.format(
        "[Review] %s — batch may exceed model context; review may truncate",
        agentName
    ));
  }

  /**
   * Logs an agent output validation retry in verbose mode.
   *
   * @param label the label
   * @param violations the violations
   * @since 1.0.0
   */
  public void validationRetry(final String label, final List<String> violations) {
    if (!isVerbose()) {
      return;
    }
    log(String.format(
        "[Review] %s — validation failed, retrying (%d violation(s))",
        label,
        violations.size()
    ));
    for (final String violation : violations) {
      log(String.format("  - %s", violation));
    }
  }

  /**
   * Logs that validation still failed after the single retry.
   *
   * @param label the label
   * @param violations the violations
   * @since 1.0.0
   */
  public void validationRetryFailed(final String label, final List<String> violations) {
    if (!isVerbose()) {
      return;
    }
    log(String.format(
        "[Review] %s — validation still failed after retry (%d violation(s))",
        label,
        violations.size()
    ));
    for (final String violation : violations) {
      log(String.format("  - %s", violation));
    }
  }

  /**
   * Logs completion of an agent review.
   * 
   * @param agentName the agentName
   * @param startedAtMs the startedAtMs
   * @since 1.0.0
   */
  public void agentComplete(final String agentName, final long startedAtMs) {
    if (isQuiet()) {
      return;
    }
    log(String.format("[Review] %s complete (%.1fs)", agentName, elapsedSeconds(startedAtMs)));
  }

  /**
   * Logs that an LLM call has started.
   * 
   * @param label the label
   * @since 1.0.0
 */
  public void llmStarted(final String label) {
    if (isQuiet()) {
      return;
    }
    log(String.format("[LLM] %s — waiting for response...", label));
  }

  /**
   * Streams a partial LLM response token to stderr.
   * 
   * @param token the token
   * @since 1.0.0
 */
  public void streamToken(final String token) {
    if (!shouldStreamLlm() || token == null || token.isEmpty()) {
      return;
    }
    System.err.print(token);
  }

  /**
   * Streams model thinking text to stderr in verbose mode.
   * 
   * @param thinking the thinking
   * @since 1.0.0
 */
  public void streamThinking(final String thinking) {
    if (!isVerbose() || thinking == null || thinking.isEmpty()) {
      return;
    }
    System.err.print(thinking);
  }

  /**
   * Ends streamed LLM output with a newline.
   * 
   * @since 1.0.0
 */
  public void streamFinished() {
    if (!shouldStreamLlm()) {
      return;
    }
    System.err.println();
  }

  /**
   * Returns the token usage ledger.
   * 
   * @return the result
   * @since 1.0.0
 */
  public LlmTokenLedger tokenLedger() {
    return tokenLedger;
  }

  /**
   * Records token usage for an LLM call.
   * 
   * @param label the label
   * @param usage the usage
   * @since 1.0.0
 */
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

  /**
   * Prints cumulative token usage summary.
   * 
   * @param model the model
   * @since 1.0.0
 */
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