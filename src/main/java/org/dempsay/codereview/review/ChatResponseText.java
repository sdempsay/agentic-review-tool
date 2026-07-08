package org.dempsay.codereview.review;

import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Extracts text from langchain4j chat responses.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class ChatResponseText {

  private ChatResponseText() {
  }

  /**
   * Extracts text from a chat response.
   * 
   * @param response the response
   * @return the result
   * @since 1.0.0
 */
  public static String extract(final ChatResponse response) {
    return response.aiMessage().text();
  }
}