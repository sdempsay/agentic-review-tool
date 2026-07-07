package org.dempsay.codereview.model;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.dempsay.codereview.cli.ReviewProgress;
import org.dempsay.codereview.config.ModelConfig;
import org.dempsay.codereview.review.ChatResponseText;

public final class StreamingLlmClient {

  private StreamingLlmClient() {
  }

  public static String complete(
      final ModelConfig model,
      final int maxTokens,
      final String prompt,
      final ReviewProgress progress,
      final String label
  ) {
    if (progress.shouldStreamLlm() && ChatModelFactory.supportsStreaming(model)) {
      return streamComplete(model, maxTokens, prompt, progress, label);
    }

    progress.llmStarted(label);
    final ChatModel chatModel = ChatModelFactory.create(model, maxTokens);
    final ChatResponse response = chatModel.chat(UserMessage.from(prompt));
    progress.recordTokenUsage(label, LlmTokenUsageExtractor.from(response));
    progress.streamFinished();
    return ChatResponseText.extract(response);
  }

  private static String streamComplete(
      final ModelConfig model,
      final int maxTokens,
      final String prompt,
      final ReviewProgress progress,
      final String label
  ) {
    progress.llmStarted(label);
    final StreamingChatModel streamingModel = ChatModelFactory.createStreaming(model, maxTokens);
    final StringBuilder responseBuilder = new StringBuilder();
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> error = new AtomicReference<>();
    final AtomicReference<ChatResponse> completedResponse = new AtomicReference<>();

    streamingModel.chat(prompt, new StreamingChatResponseHandler() {
      @Override
      public void onPartialResponse(final String partialResponse) {
        progress.streamToken(partialResponse);
        responseBuilder.append(partialResponse);
      }

      @Override
      public void onPartialThinking(final PartialThinking partialThinking) {
        progress.streamThinking(partialThinking.text());
      }

      @Override
      public void onCompleteResponse(final ChatResponse response) {
        completedResponse.set(response);
        if (responseBuilder.isEmpty()) {
          final String text = ChatResponseText.extract(response);
          progress.streamToken(text);
          responseBuilder.append(text);
        }
        latch.countDown();
      }

      @Override
      public void onError(final Throwable throwable) {
        error.set(throwable);
        latch.countDown();
      }
    });

    awaitCompletion(latch, model);
    progress.recordTokenUsage(label, LlmTokenUsageExtractor.from(completedResponse.get()));
    progress.streamFinished();

    if (error.get() != null) {
      throw new IllegalStateException("Streaming LLM call failed for " + label, error.get());
    }
    return responseBuilder.toString();
  }

  private static void awaitCompletion(final CountDownLatch latch, final ModelConfig model) {
    try {
      final boolean completed = latch.await(model.resolveTimeout().toSeconds(), TimeUnit.SECONDS);
      if (!completed) {
        throw new IllegalStateException(
            "Streaming LLM call timed out after " + model.resolveTimeout().toSeconds() + "s"
        );
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Streaming LLM call interrupted", exception);
    }
  }
}