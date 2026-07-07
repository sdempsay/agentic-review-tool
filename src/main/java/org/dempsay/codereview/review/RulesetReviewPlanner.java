package org.dempsay.codereview.review;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

public final class RulesetReviewPlanner {

  private record PlanOptions(
      AppConfig config,
      int contextTokens,
      int maxAgentDiffKb,
      int maxFilesPerAgent,
      ReviewContentMode contentMode
  ) {
  }

  private RulesetReviewPlanner() {
  }

  public static List<RulesetReviewTask> plan(
      final List<Rule> rules,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles
  ) {
    return plan(rules, classification, changedFiles, 0, 0);
  }

  public static List<RulesetReviewTask> plan(
      final List<Rule> rules,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles,
      final int maxAgentDiffKb,
      final int maxFilesPerAgent
  ) {
    return plan(
        rules,
        classification,
        changedFiles,
        new PlanOptions(null, 0, maxAgentDiffKb, maxFilesPerAgent, ReviewContentMode.DIFF)
    );
  }

  public static List<RulesetReviewTask> plan(
      final List<Rule> rules,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles,
      final AppConfig config,
      final int contextTokens
  ) {
    return plan(rules, classification, changedFiles, config, contextTokens, ReviewContentMode.DIFF);
  }

  public static List<RulesetReviewTask> plan(
      final List<Rule> rules,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles,
      final AppConfig config,
      final int contextTokens,
      final ReviewContentMode contentMode
  ) {
    return plan(
        rules,
        classification,
        changedFiles,
        new PlanOptions(config, contextTokens, config.maxAgentDiffKb(), config.maxFilesPerAgent(), contentMode)
    );
  }

  private static List<RulesetReviewTask> plan(
      final List<Rule> rules,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles,
      final PlanOptions options
  ) {
    final List<RulesetReviewTask> tasks = new ArrayList<>();
    final Set<String> filesWithRules = new HashSet<>();

    for (final Rule rule : rules) {
      final List<ChangedFile> filesForRule = collectFilesForRule(rule, classification, changedFiles);
      if (!filesForRule.isEmpty()) {
        addBatchedRuleTasks(tasks, rule, filesForRule, options);
        filesForRule.stream().map(ChangedFile::path).forEach(filesWithRules::add);
      }
    }

    final List<ChangedFile> unmatchedFiles = collectUnmatchedFiles(changedFiles, filesWithRules);
    if (!unmatchedFiles.isEmpty()) {
      addBatchedGeneralTasks(tasks, unmatchedFiles, options);
    }

    return List.copyOf(tasks);
  }

  private static void addBatchedRuleTasks(
      final List<RulesetReviewTask> tasks,
      final Rule rule,
      final List<ChangedFile> filesForRule,
      final PlanOptions options
  ) {
    final AgentBatchLimits limits = resolveLimits(options, rule, false);
    final List<RulesetBatchSplitter.BatchChunk> batches = RulesetBatchSplitter.split(filesForRule, limits);
    for (int index = 0; index < batches.size(); index++) {
      final RulesetBatchSplitter.BatchChunk batch = batches.get(index);
      tasks.add(RulesetReviewTask.forRule(
          rule,
          batch.files(),
          index + 1,
          batches.size(),
          batch.exceedsContextCap()
      ));
    }
  }

  private static void addBatchedGeneralTasks(
      final List<RulesetReviewTask> tasks,
      final List<ChangedFile> unmatchedFiles,
      final PlanOptions options
  ) {
    final AgentBatchLimits limits = resolveLimits(options, null, true);
    final List<RulesetBatchSplitter.BatchChunk> batches = RulesetBatchSplitter.split(unmatchedFiles, limits);
    for (int index = 0; index < batches.size(); index++) {
      final RulesetBatchSplitter.BatchChunk batch = batches.get(index);
      tasks.add(RulesetReviewTask.generalFallback(
          batch.files(),
          index + 1,
          batches.size(),
          batch.exceedsContextCap()
      ));
    }
  }

  private static AgentBatchLimits resolveLimits(
      final PlanOptions options,
      final Rule rule,
      final boolean general
  ) {
    if (options.config() != null && options.contextTokens() > 0) {
      return general
          ? AgentBatchLimits.forGeneral(options.config(), options.contextTokens(), options.contentMode())
          : AgentBatchLimits.forRuleset(options.config(), options.contextTokens(), rule, options.contentMode());
    }
    return AgentBatchLimits.fromLegacyCaps(
        options.maxAgentDiffKb(),
        options.maxFilesPerAgent(),
        general
            ? PromptBudgetEstimator.generalOverheadBytes(options.contentMode())
            : PromptBudgetEstimator.rulesetOverheadBytes(rule, options.contentMode())
    );
  }

  private static List<ChangedFile> collectFilesForRule(
      final Rule rule,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles
  ) {
    final List<ChangedFile> filesForRule = new ArrayList<>();
    for (final ChangedFile file : changedFiles) {
      if (!file.hasDiff()) {
        continue;
      }
      if (classification.getOrDefault(file.path(), List.of()).contains(rule)) {
        filesForRule.add(file);
      }
    }
    return filesForRule;
  }

  private static List<ChangedFile> collectUnmatchedFiles(
      final List<ChangedFile> changedFiles,
      final Set<String> filesWithRules
  ) {
    final List<ChangedFile> unmatchedFiles = new ArrayList<>();
    for (final ChangedFile file : changedFiles) {
      if (file.hasDiff() && !filesWithRules.contains(file.path())) {
        unmatchedFiles.add(file);
      }
    }
    return unmatchedFiles;
  }
}