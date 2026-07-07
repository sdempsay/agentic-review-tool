package org.dempsay.codereview.review;

import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.model.ChatModelFactory;
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
      final List<ChangedFile> changedFiles
  ) {
    return ExceptionalSupport.supply(() -> reviewRequired(config, rules, changedFiles));
  }

  public static String reviewRequired(
      final AppConfig config,
      final List<Rule> rules,
      final List<ChangedFile> changedFiles
  ) {
    final List<String> filePaths = changedFiles.stream().map(ChangedFile::path).toList();
    final Map<String, List<Rule>> classification = RulesClassifier.classify(rules, filePaths);
    final List<RulesetReviewTask> tasks = RulesetReviewPlanner.plan(rules, classification, changedFiles);
    if (tasks.isEmpty()) {
      return "No reviewable diffs found.";
    }

    final ChatModel chatModel = ChatModelFactory.create(config.model(), config.maxTokens());
    final List<ReviewResult> results = new ArrayList<>();
    for (final RulesetReviewTask task : tasks) {
      final String prompt = task.isGeneralFallback()
          ? ReviewPromptBuilder.buildGeneralFallback(task.files())
          : ReviewPromptBuilder.buildForRuleset(task.rule(), task.files());
      results.add(new ReviewResult(task.agentName(), chatModel.chat(prompt)));
    }
    return ReviewAggregator.aggregate(results);
  }
}