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
    return plan(rules, classification, changedFiles, null, 0, maxAgentDiffKb, maxFilesPerAgent);
  }

  public static List<RulesetReviewTask> plan(
      final List<Rule> rules,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles,
      final AppConfig config,
      final int contextTokens
  ) {
    return plan(
        rules,
        classification,
        changedFiles,
        config,
        contextTokens,
        config.maxAgentDiffKb(),
        config.maxFilesPerAgent()
    );
  }

  private static List<RulesetReviewTask> plan(
      final List<Rule> rules,
      final Map<String, List<Rule>> classification,
      final List<ChangedFile> changedFiles,
      final AppConfig config,
      final int contextTokens,
      final int maxAgentDiffKb,
      final int maxFilesPerAgent
  ) {
    final List<RulesetReviewTask> tasks = new ArrayList<>();
    final Set<String> filesWithRules = new HashSet<>();

    for (final Rule rule : rules) {
      final List<ChangedFile> filesForRule = collectFilesForRule(rule, classification, changedFiles);
      if (!filesForRule.isEmpty()) {
        addBatchedRuleTasks(tasks, rule, filesForRule, config, contextTokens, maxAgentDiffKb, maxFilesPerAgent);
        filesForRule.stream().map(ChangedFile::path).forEach(filesWithRules::add);
      }
    }

    final List<ChangedFile> unmatchedFiles = collectUnmatchedFiles(changedFiles, filesWithRules);
    if (!unmatchedFiles.isEmpty()) {
      addBatchedGeneralTasks(tasks, unmatchedFiles, config, contextTokens, maxAgentDiffKb, maxFilesPerAgent);
    }

    return List.copyOf(tasks);
  }

  private static void addBatchedRuleTasks(
      final List<RulesetReviewTask> tasks,
      final Rule rule,
      final List<ChangedFile> filesForRule,
      final AppConfig config,
      final int contextTokens,
      final int maxAgentDiffKb,
      final int maxFilesPerAgent
  ) {
    final AgentBatchLimits limits = resolveLimits(config, contextTokens, maxAgentDiffKb, maxFilesPerAgent, rule, false);
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
      final AppConfig config,
      final int contextTokens,
      final int maxAgentDiffKb,
      final int maxFilesPerAgent
  ) {
    final AgentBatchLimits limits = resolveLimits(config, contextTokens, maxAgentDiffKb, maxFilesPerAgent, null, true);
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
      final AppConfig config,
      final int contextTokens,
      final int maxAgentDiffKb,
      final int maxFilesPerAgent,
      final Rule rule,
      final boolean general
  ) {
    if (config != null && contextTokens > 0) {
      return general
          ? AgentBatchLimits.forGeneral(config, contextTokens)
          : AgentBatchLimits.forRuleset(config, contextTokens, rule);
    }
    return AgentBatchLimits.fromLegacyCaps(
        maxAgentDiffKb,
        maxFilesPerAgent,
        general ? PromptBudgetEstimator.generalOverheadBytes() : PromptBudgetEstimator.rulesetOverheadBytes(rule)
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