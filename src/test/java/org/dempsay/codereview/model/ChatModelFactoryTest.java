package org.dempsay.codereview.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.dempsay.codereview.config.ModelConfig;
import org.junit.Test;

public class ChatModelFactoryTest {

  @Test
  public void createOllamaChatModel() {
    final ModelConfig model = new ModelConfig("ollama", "qwen3", 0.2, "http://localhost:11434", 120, null);

    final ChatModel chatModel = ChatModelFactory.create(model, 4096);

    assertNotNull(chatModel);
  }

  @Test
  public void createOllamaStreamingChatModel() {
    final ModelConfig model = new ModelConfig("ollama", "qwen3", 0.2, "http://localhost:11434", 120, null);

    assertTrue(ChatModelFactory.supportsStreaming(model));
    final StreamingChatModel streamingModel = ChatModelFactory.createStreaming(model, 4096);

    assertNotNull(streamingModel);
  }

  @Test
  public void createOpenRouterChatModel() {
    final ModelConfig model = new ModelConfig(
        "openrouter",
        "anthropic/claude-sonnet-4",
        0.2,
        null,
        120,
        "test-api-key"
    );

    assertTrue(ChatModelFactory.supportsStreaming(model));
    assertNotNull(ChatModelFactory.create(model, 4096));
    assertNotNull(ChatModelFactory.createStreaming(model, 4096));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectUnsupportedProvider() {
    final ModelConfig model = new ModelConfig("openai", "gpt-4", 0.2, null, 0, null);
    ChatModelFactory.create(model, 4096);
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectOpenRouterWithoutApiKey() {
    final ModelConfig model = new ModelConfig("openrouter", "anthropic/claude-sonnet-4", 0.2, null, 0, null);
    ChatModelFactory.create(model, 4096);
  }
}