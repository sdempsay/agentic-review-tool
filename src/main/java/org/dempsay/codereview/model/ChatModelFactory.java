package org.dempsay.codereview.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.dempsay.codereview.config.ModelConfig;

public final class ChatModelFactory {

  private ChatModelFactory() {
  }

  public static ChatModel create(final ModelConfig model, final int maxTokens) {
    if ("ollama".equalsIgnoreCase(model.provider())) {
      return OllamaChatModel.builder()
          .baseUrl(model.resolveBaseUrl())
          .modelName(model.name())
          .temperature(model.temperature())
          .numPredict(maxTokens)
          .timeout(model.resolveTimeout())
          .build();
    }
    throw new IllegalArgumentException("Unsupported model provider: " + model.provider());
  }

  public static boolean supportsStreaming(final ModelConfig model) {
    return "ollama".equalsIgnoreCase(model.provider());
  }

  public static StreamingChatModel createStreaming(final ModelConfig model, final int maxTokens) {
    if ("ollama".equalsIgnoreCase(model.provider())) {
      return OllamaStreamingChatModel.builder()
          .baseUrl(model.resolveBaseUrl())
          .modelName(model.name())
          .temperature(model.temperature())
          .numPredict(maxTokens)
          .timeout(model.resolveTimeout())
          .build();
    }
    throw new IllegalArgumentException("Unsupported streaming model provider: " + model.provider());
  }
}