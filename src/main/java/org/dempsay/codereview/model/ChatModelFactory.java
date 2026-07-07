package org.dempsay.codereview.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.dempsay.codereview.config.ModelConfig;

public final class ChatModelFactory {

  private ChatModelFactory() {
  }

  public static ChatModel create(final ModelConfig model, final int maxTokens) {
    if (model.isOllama()) {
      return OllamaChatModel.builder()
          .baseUrl(model.resolveBaseUrl())
          .modelName(model.name())
          .temperature(model.temperature())
          .numPredict(maxTokens)
          .timeout(model.resolveTimeout())
          .build();
    }
    if (model.isOpenRouter()) {
      return openAiBuilder(model, maxTokens).build();
    }
    throw new IllegalArgumentException("Unsupported model provider: " + model.provider());
  }

  public static boolean supportsStreaming(final ModelConfig model) {
    return model.isOllama() || model.isOpenRouter();
  }

  public static StreamingChatModel createStreaming(final ModelConfig model, final int maxTokens) {
    if (model.isOllama()) {
      return OllamaStreamingChatModel.builder()
          .baseUrl(model.resolveBaseUrl())
          .modelName(model.name())
          .temperature(model.temperature())
          .numPredict(maxTokens)
          .timeout(model.resolveTimeout())
          .build();
    }
    if (model.isOpenRouter()) {
      return openAiStreamingBuilder(model, maxTokens).build();
    }
    throw new IllegalArgumentException("Unsupported streaming model provider: " + model.provider());
  }

  private static OpenAiChatModel.OpenAiChatModelBuilder openAiBuilder(
      final ModelConfig model,
      final int maxTokens
  ) {
    return OpenAiChatModel.builder()
        .baseUrl(model.resolveBaseUrl())
        .apiKey(requireApiKey(model))
        .modelName(model.name())
        .temperature(model.temperature())
        .maxTokens(maxTokens)
        .timeout(model.resolveTimeout());
  }

  private static OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder openAiStreamingBuilder(
      final ModelConfig model,
      final int maxTokens
  ) {
    return OpenAiStreamingChatModel.builder()
        .baseUrl(model.resolveBaseUrl())
        .apiKey(requireApiKey(model))
        .modelName(model.name())
        .temperature(model.temperature())
        .maxTokens(maxTokens)
        .timeout(model.resolveTimeout());
  }

  static String requireApiKey(final ModelConfig model) {
    final String apiKey = model.resolveApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalArgumentException(
          "OpenRouter API key is required. Set OPENROUTER_API_KEY or model.apiKey in config.json"
      );
    }
    return apiKey;
  }
}