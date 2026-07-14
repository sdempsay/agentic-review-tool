package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.cli.ReviewProgress;
import org.dempsay.codereview.config.ModelConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.model.StreamingLlmClient;

/**
 * Completes agent review prompts with output validation and a single retry.
 *
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class GuardedLlmCompletion {

  private static final String RETRY_HINT = """

      FORMAT CORRECTION (previous response failed validation):
      - One bullet per finding: path:line — [must-fix|nit] — brief description
      - Cite only + lines present in the diff; paths must stay in scope
      - No re-evaluation narrative; do not enumerate clean files when findings exist
      - Use must-fix only when this ruleset allows it
      """;

  @FunctionalInterface
  interface LlmCompleter {
    /**
     * Completes an LLM prompt.
     *
     * @param model the model
     * @param maxTokens the maxTokens
     * @param prompt the prompt
     * @param progress the progress
     * @param label the label
     * @return the response text
     * @since 1.0.0
     */
    String complete(
        ModelConfig model,
        int maxTokens,
        String prompt,
        ReviewProgress progress,
        String label
    );
  }

  private GuardedLlmCompletion() {
  }

  /**
   * Completes an agent review with validation and at most one retry.
   *
   * @param model the model
   * @param maxTokens the maxTokens
   * @param prompt the prompt
   * @param progress the progress
   * @param label the label
   * @param agentName the agentName
   * @param scopedFiles the scopedFiles
   * @return the response text
   * @since 1.0.0
   */
  public static String completeAgentReview(
      final ModelConfig model,
      final int maxTokens,
      final String prompt,
      final ReviewProgress progress,
      final String label,
      final String agentName,
      final List<ChangedFile> scopedFiles
  ) {
    return completeAgentReview(
        new AgentReviewRequest(model, maxTokens, prompt, progress, label, agentName, scopedFiles),
        StreamingLlmClient::complete
    );
  }

  static String completeAgentReview(final AgentReviewRequest request, final LlmCompleter completer) {
    final String firstResponse = completer.complete(
        request.model(),
        request.maxTokens(),
        request.prompt(),
        request.progress(),
        request.label()
    );
    final ReviewOutputValidator.ValidationResult firstValidation = ReviewOutputValidator.validate(
        request.agentName(),
        request.scopedFiles(),
        firstResponse
    );
    if (firstValidation.valid()) {
      return firstResponse;
    }

    request.progress().validationRetry(request.label(), firstValidation.violations());
    final String retryPrompt = request.prompt()
        + RETRY_HINT
        + "\nViolations:\n"
        + firstValidation.violationSummary();
    final String retryLabel = request.label() + " (retry)";
    final String retryResponse = completer.complete(
        request.model(),
        request.maxTokens(),
        retryPrompt,
        request.progress(),
        retryLabel
    );
    final ReviewOutputValidator.ValidationResult retryValidation = ReviewOutputValidator.validate(
        request.agentName(),
        request.scopedFiles(),
        retryResponse
    );
    if (!retryValidation.valid()) {
      request.progress().validationRetryFailed(request.label(), retryValidation.violations());
    }
    return retryResponse;
  }
}