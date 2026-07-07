package org.dempsay.codereview.review;

import dev.langchain4j.model.chat.ChatModel;
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
    final long reviewable = changedFiles.stream().filter(ChangedFile::hasDiff).count();
    if (reviewable == 0) {
      return "No reviewable diffs found.";
    }

    final List<String> filePaths = changedFiles.stream().map(ChangedFile::path).toList();
    final Map<String, List<Rule>> classification = RulesClassifier.classify(rules, filePaths);
    final ChatModel chatModel = ChatModelFactory.create(config.model(), config.maxTokens());
    final String prompt = ReviewPromptBuilder.build(classification, changedFiles);
    return chatModel.chat(prompt);
  }
}