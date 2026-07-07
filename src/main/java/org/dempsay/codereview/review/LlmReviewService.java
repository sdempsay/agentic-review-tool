package org.dempsay.codereview.review;

import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.model.ChatModelFactory;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class LlmReviewService {

  private LlmReviewService() {
  }

  public static ExceptionalResponse<String> review(final AppConfig config, final List<ChangedFile> changedFiles) {
    return ExceptionalSupport.supply(() -> reviewRequired(config, changedFiles));
  }

  public static String reviewRequired(final AppConfig config, final List<ChangedFile> changedFiles) {
    final long reviewable = changedFiles.stream().filter(ChangedFile::hasDiff).count();
    if (reviewable == 0) {
      return "No reviewable diffs found.";
    }

    final ChatModel chatModel = ChatModelFactory.create(config.model(), config.maxTokens());
    final String prompt = ReviewPromptBuilder.build(changedFiles);
    return chatModel.chat(prompt);
  }
}