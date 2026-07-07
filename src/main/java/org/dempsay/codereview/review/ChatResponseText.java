package org.dempsay.codereview.review;

import dev.langchain4j.model.chat.response.ChatResponse;

public final class ChatResponseText {

  private ChatResponseText() {
  }

  public static String extract(final ChatResponse response) {
    return response.aiMessage().text();
  }
}