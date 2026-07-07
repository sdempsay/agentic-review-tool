package org.dempsay.codereview.review;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.cli.ReviewProgress;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.model.OllamaModelInspector;
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
    final ReviewContentMode contentMode = ReviewContentMode.resolve(changedFiles);
    final long reviewStageStart = System.currentTimeMillis();
    progress.stageStart("Review");

    final List<String> filePaths = changedFiles.stream().map(ChangedFile::path).toList();
    final Map<String, List<Rule>> classification = RulesClassifier.classify(rules, filePaths);
    final int contextTokens = OllamaModelInspector.resolveContextTokens(config.model());
    if (contextTokens <= 0 && !progress.isQuiet()) {
      progress.batchCapFallback(config.maxAgentDiffKb());
    } else if (contextTokens > 0 && !progress.isQuiet()) {
      progress.batchCapResolved(contextTokens, config.maxAgentDiffKb());
    }
    final List<RulesetReviewTask> tasks = RulesetReviewPlanner.plan(
        rules,
        classification,
        changedFiles,
        config,
        contextTokens,
        contentMode
    );
    if (tasks.isEmpty()) {
      progress.stageComplete("Review", reviewStageStart);
      return "No reviewable files found.";
    }

    final List<ReviewResult> agentResults = runAgentReviews(config, tasks, progress, contentMode);
    progress.stageComplete("Review", reviewStageStart);

    final long summarizeStageStart = System.currentTimeMillis();
    progress.stageStart("Summarize");
    final String summary = LlmSummarizeService.summarizeRequired(config, agentResults, changedFiles, progress, contentMode);
    progress.stageComplete("Summarize", summarizeStageStart);
    progress.printTokenSummary(config.model());

    return ReviewReportComposer.compose(agentResults, summary, progress.tokenLedger(), config.model());
  }

  private static List<ReviewResult> runAgentReviews(
      final AppConfig config,
      final List<RulesetReviewTask> tasks,
      final ReviewProgress progress,
      final ReviewContentMode contentMode
  ) {
    final ReviewPromptSupplements supplements =
        ExceptionalSupport.response(ReviewPromptSupplements.load(config.rulesDir()));
    final List<ReviewResult> results = new ArrayList<>();
    for (final RulesetReviewTask task : tasks) {
      final long agentStart = System.currentTimeMillis();
      final List<String> scopedFiles = task.files().stream().map(ChangedFile::path).toList();
      progress.agentStart(task.agentName(), scopedFiles.size(), scopedFiles);
      if (task.exceedsContextCap()) {
        progress.agentContextWarning(task.agentName());
      }

      final String prompt = task.isGeneralFallback()
          ? ReviewPromptBuilder.buildGeneralFallback(task.files(), contentMode, supplements)
          : ReviewPromptBuilder.buildForRuleset(task.rule(), task.files(), contentMode, supplements);
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