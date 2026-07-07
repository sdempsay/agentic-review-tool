package org.dempsay.codereview.review;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.cli.ReviewProgress;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.model.StreamingLlmClient;
import org.dempsay.codereview.rules.Rule;
import org.dempsay.codereview.rules.RulesClassifier;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class LlmReviewService {

  private LlmReviewService() {
  }

  public static ExceptionalResponse<String> review(
      final AppConfig config,
      final List<Rule> rules,
      final List<ChangedFile> changedFiles,
      final ReviewProgress progress
  ) {
    return ExceptionalSupport.supply(() -> reviewRequired(config, rules, changedFiles, progress));
  }

  public static String reviewRequired(
      final AppConfig config,
      final List<Rule> rules,
      final List<ChangedFile> changedFiles,
      final ReviewProgress progress
  ) {
    final long reviewStageStart = System.currentTimeMillis();
    progress.stageStart("Review");

    final List<String> filePaths = changedFiles.stream().map(ChangedFile::path).toList();
    final Map<String, List<Rule>> classification = RulesClassifier.classify(rules, filePaths);
    final List<RulesetReviewTask> tasks = RulesetReviewPlanner.plan(rules, classification, changedFiles);
    if (tasks.isEmpty()) {
      progress.stageComplete("Review", reviewStageStart);
      return "No reviewable diffs found.";
    }

    final List<ReviewResult> agentResults = runAgentReviews(config, tasks, progress);
    progress.stageComplete("Review", reviewStageStart);

    final long summarizeStageStart = System.currentTimeMillis();
    progress.stageStart("Summarize");
    final String summary = LlmSummarizeService.summarizeRequired(config, agentResults, changedFiles, progress);
    progress.stageComplete("Summarize", summarizeStageStart);

    return ReviewReportComposer.compose(agentResults, summary);
  }

  private static List<ReviewResult> runAgentReviews(
      final AppConfig config,
      final List<RulesetReviewTask> tasks,
      final ReviewProgress progress
  ) {
    final List<ReviewResult> results = new ArrayList<>();
    for (final RulesetReviewTask task : tasks) {
      final long agentStart = System.currentTimeMillis();
      final List<String> scopedFiles = task.files().stream().map(ChangedFile::path).toList();
      progress.agentStart(task.agentName(), scopedFiles.size(), scopedFiles);

      final String prompt = task.isGeneralFallback()
          ? ReviewPromptBuilder.buildGeneralFallback(task.files())
          : ReviewPromptBuilder.buildForRuleset(task.rule(), task.files());
      final String findings = StreamingLlmClient.complete(
          config.model(),
          config.maxTokens(),
          prompt,
          progress,
          task.agentName()
      );
      results.add(new ReviewResult(task.agentName(), findings));
      progress.agentComplete(task.agentName(), agentStart);
    }
    return results;
  }
}