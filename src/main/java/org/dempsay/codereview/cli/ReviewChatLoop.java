package org.dempsay.codereview.cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.dempsay.codereview.review.ReviewChatOrchestrator;
import org.dempsay.codereview.review.ReviewPromptSupplements;
import org.dempsay.codereview.review.ReviewSessionContext;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class ReviewChatLoop {

  private ReviewChatLoop() {
  }

  public static ExceptionalResponse<Void> run(final ReviewSessionContext session) {
    return run(session, null);
  }

  public static ExceptionalResponse<Void> run(
      final ReviewSessionContext session,
      final ExceptionalListener listener
  ) {
    return ReviewPromptSupplements.load(session.config().rulesDir(), listener)
        .chain((loadListener, supplements) -> ExceptionalSupport.supply(() -> {
      System.out.println();
      System.out.println("--- Follow-up Chat ---");
      System.out.println("Ask questions about this review (exit to end).");

      final ReviewChatOrchestrator orchestrator = new ReviewChatOrchestrator(session, supplements);
      final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

      while (true) {
        System.out.print(System.lineSeparator() + "You> ");
        final String line = reader.readLine();
        if (line == null || isExit(line.trim())) {
          break;
        }
        if (line.isBlank()) {
          continue;
        }

        System.out.println();
        System.out.println(orchestrator.respond(line.trim()));
      }
      return null;
    }, loadListener), listener);
  }

  public static boolean isExit(final String input) {
    return "exit".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input);
  }

  public static boolean shouldEnable(final Boolean chatFlag, final boolean noChat) {
    if (noChat) {
      return false;
    }
    if (chatFlag != null) {
      return chatFlag;
    }
    return System.console() != null;
  }
}