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
import org.dempsay.utils.exceptional.api.ExceptionalListener;
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
    return review(config, rules, changedFiles, progress, null);
  }

  public static ExceptionalResponse<String> review(
      final AppConfig config,
      final List<Rule> rules,
      final List<ChangedFile> changedFiles,
      final ReviewProgress progress,
      final ExceptionalListener listener
  ) {
    return ReviewPromptSupplements.load(config.rulesDir(), listener)
        .chain((loadListener, supplements) ->
            ExceptionalSupport.supply(() -> runReviewPhase(config, rules, changedFiles, progress, supplements), loadListener)
                .chain((phaseListener, phase) -> completeReview(config, phase, progress, loadListener), loadListener),
            listener
        );
  }

  private static ReviewPhase runReviewPhase(
      final AppConfig config,
      final List<Rule> rules,
      final List<ChangedFile> changedFiles,
      final ReviewProgress progress,
      final ReviewPromptSupplements supplements
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
        contentMode,
        supplements
    );
    if (tasks.isEmpty()) {
      progress.stageComplete("Review", reviewStageStart);
      return ReviewPhase.shortCircuit("No reviewable files found.");
    }

    final List<ReviewResult> agentResults = runAgentReviews(config, tasks, progress, contentMode, supplements);
    progress.stageComplete("Review", reviewStageStart);

    final long summarizeStageStart = System.currentTimeMillis();
    progress.stageStart("Summarize");
    return ReviewPhase.ready(agentResults, changedFiles, contentMode, summarizeStageStart);
  }

  private static ExceptionalResponse<String> completeReview(
      final AppConfig config,
      final ReviewPhase phase,
      final ReviewProgress progress,
      final ExceptionalListener listener
  ) {
    if (phase.shortCircuitMessage() != null) {
      return ExceptionalResponse.success(phase.shortCircuitMessage());
    }

    return LlmSummarizeService.summarize(
        config,
        phase.agentResults(),
        phase.changedFiles(),
        progress,
        phase.contentMode(),
        listener
    ).chain((summarizeListener, summary) -> {
      progress.stageComplete("Summarize", phase.summarizeStageStart());
      progress.printTokenSummary(config.model());
      return ExceptionalResponse.success(
          ReviewReportComposer.compose(
              phase.agentResults(),
              summary,
              progress.tokenLedger(),
              config.model()
          )
      );
    }, listener);
  }

  private static List<ReviewResult> runAgentReviews(
      final AppConfig config,
      final List<RulesetReviewTask> tasks,
      final ReviewProgress progress,
      final ReviewContentMode contentMode,
      final ReviewPromptSupplements supplements
  ) {
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
          config.resolvedReviewMaxTokens(),
          prompt,
          progress,
          task.agentName()
      );
      results.add(new ReviewResult(task.agentName(), findings));
      progress.agentComplete(task.agentName(), agentStart);
    }
    return results;
  }

  private record ReviewPhase(
      String shortCircuitMessage,
      List<ReviewResult> agentResults,
      ReviewContentMode contentMode,
      long summarizeStageStart,
      List<ChangedFile> changedFiles
  ) {
    static ReviewPhase shortCircuit(final String message) {
      return new ReviewPhase(message, List.of(), null, 0L, List.of());
    }

    static ReviewPhase ready(
        final List<ReviewResult> agentResults,
        final List<ChangedFile> changedFiles,
        final ReviewContentMode contentMode,
        final long summarizeStageStart
    ) {
      return new ReviewPhase(null, agentResults, contentMode, summarizeStageStart, changedFiles);
    }
  }
}