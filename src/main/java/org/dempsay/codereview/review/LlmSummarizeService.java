package org.dempsay.codereview.review;

import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.model.ChatModelFactory;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class LlmSummarizeService {

  private LlmSummarizeService() {
  }

  public static ExceptionalResponse<String> summarize(
      final AppConfig config,
      final List<ReviewResult> agentResults,
      final List<ChangedFile> changedFiles
  ) {
    return ExceptionalSupport.supply(() -> summarizeRequired(config, agentResults, changedFiles));
  }

  public static String summarizeRequired(
      final AppConfig config,
      final List<ReviewResult> agentResults,
      final List<ChangedFile> changedFiles
  ) {
    final ChatModel chatModel = ChatModelFactory.create(config.model(), config.maxTokens());
    final String prompt = SummarizePromptBuilder.build(agentResults, changedFiles);
    return chatModel.chat(prompt).trim();
  }
}