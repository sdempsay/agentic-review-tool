package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.cli.ReviewProgress;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.model.StreamingLlmClient;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/**
 * Summarizes agent findings into a final assessment.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class LlmSummarizeService {

  private LlmSummarizeService() {
  }

  /**
   * Summarizes agent findings into a final assessment.
   * 
   * @param config the config
   * @param agentResults the agentResults
   * @param changedFiles the changedFiles
   * @param progress the progress
   * @return the result
   * @since 1.0.0
 */
  public static ExceptionalResponse<String> summarize(
      final AppConfig config,
      final List<ReviewResult> agentResults,
      final List<ChangedFile> changedFiles,
      final ReviewProgress progress
  ) {
    return summarize(config, agentResults, changedFiles, progress, ReviewContentMode.resolve(changedFiles));
  }

  /**
   * Summarizes agent findings into a final assessment.
   * 
   * @param config the config
   * @param agentResults the agentResults
   * @param changedFiles the changedFiles
   * @param progress the progress
   * @param contentMode the contentMode
   * @return the result
   * @since 1.0.0
 */
  public static ExceptionalResponse<String> summarize(
      final AppConfig config,
      final List<ReviewResult> agentResults,
      final List<ChangedFile> changedFiles,
      final ReviewProgress progress,
      final ReviewContentMode contentMode
  ) {
    return summarize(config, agentResults, changedFiles, progress, contentMode, null);
  }

  /**
   * Summarizes agent findings into a final assessment.
   * 
   * @param config the config
   * @param agentResults the agentResults
   * @param changedFiles the changedFiles
   * @param progress the progress
   * @param contentMode the contentMode
   * @param listener the listener
   * @return the result
   * @since 1.0.0
 */
  public static ExceptionalResponse<String> summarize(
      final AppConfig config,
      final List<ReviewResult> agentResults,
      final List<ChangedFile> changedFiles,
      final ReviewProgress progress,
      final ReviewContentMode contentMode,
      final ExceptionalListener listener
  ) {
    return ReviewPromptSupplements.load(config.rulesDir(), listener)
        .chain((loadListener, supplements) -> ExceptionalSupport.supply(() -> {
          final String prompt = SummarizePromptBuilder.build(
              agentResults,
              changedFiles,
              contentMode,
              supplements.guardrails()
          );
          return StreamingLlmClient.complete(
              config.model(),
              config.resolvedReviewMaxTokens(),
              prompt,
              progress,
              "summarize"
          ).trim();
        }, loadListener), listener);
  }
}
