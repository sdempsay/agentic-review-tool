package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.cli.ReviewProgress;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.model.StreamingLlmClient;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class LlmSummarizeService {

  private LlmSummarizeService() {
  }

  public static ExceptionalResponse<String> summarize(
      final AppConfig config,
      final List<ReviewResult> agentResults,
      final List<ChangedFile> changedFiles,
      final ReviewProgress progress
  ) {
    return ExceptionalSupport.supply(() -> summarizeRequired(config, agentResults, changedFiles, progress));
  }

  public static String summarizeRequired(
      final AppConfig config,
      final List<ReviewResult> agentResults,
      final List<ChangedFile> changedFiles,
      final ReviewProgress progress
  ) {
    return summarizeRequired(config, agentResults, changedFiles, progress, ReviewContentMode.resolve(changedFiles));
  }

  public static String summarizeRequired(
      final AppConfig config,
      final List<ReviewResult> agentResults,
      final List<ChangedFile> changedFiles,
      final ReviewProgress progress,
      final ReviewContentMode contentMode
  ) {
    final String prompt = SummarizePromptBuilder.build(agentResults, changedFiles, contentMode);
    return StreamingLlmClient.complete(
        config.model(),
        config.maxTokens(),
        prompt,
        progress,
        "summarize"
    ).trim();
  }
}