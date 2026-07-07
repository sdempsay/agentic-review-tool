package org.dempsay.codereview.review;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    final List<RulesetReviewTask> tasks = new ArrayList<>();
    final Set<String> filesWithRules = new HashSet<>();

    for (final Rule rule : rules) {
      final List<ChangedFile> filesForRule = collectFilesForRule(rule, classification, changedFiles);
      if (!filesForRule.isEmpty()) {
        addBatchedRuleTasks(tasks, rule, filesForRule, maxAgentDiffKb, maxFilesPerAgent);
        filesForRule.stream().map(ChangedFile::path).forEach(filesWithRules::add);
      }
    }

    final List<ChangedFile> unmatchedFiles = collectUnmatchedFiles(changedFiles, filesWithRules);
    if (!unmatchedFiles.isEmpty()) {
      addBatchedGeneralTasks(tasks, unmatchedFiles, maxAgentDiffKb, maxFilesPerAgent);
    }

    return List.copyOf(tasks);
  }

  private static void addBatchedRuleTasks(
      final List<RulesetReviewTask> tasks,
      final Rule rule,
      final List<ChangedFile> filesForRule,
      final int maxAgentDiffKb,
      final int maxFilesPerAgent
  ) {
    final List<List<ChangedFile>> batches = RulesetBatchSplitter.split(filesForRule, maxAgentDiffKb, maxFilesPerAgent);
    for (int index = 0; index < batches.size(); index++) {
      tasks.add(RulesetReviewTask.forRule(rule, batches.get(index), index + 1, batches.size()));
    }
  }

  private static void addBatchedGeneralTasks(
      final List<RulesetReviewTask> tasks,
      final List<ChangedFile> unmatchedFiles,
      final int maxAgentDiffKb,
      final int maxFilesPerAgent
  ) {
    final List<List<ChangedFile>> batches = RulesetBatchSplitter.split(unmatchedFiles, maxAgentDiffKb, maxFilesPerAgent);
    for (int index = 0; index < batches.size(); index++) {
      tasks.add(RulesetReviewTask.generalFallback(batches.get(index), index + 1, batches.size()));
    }
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