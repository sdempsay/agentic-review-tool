package org.dempsay.codereview.cli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.config.ModelConfig;
import org.dempsay.codereview.review.ReviewSessionContext;
import org.dempsay.codereview.rules.Rule;
import org.junit.Test;

public class ReviewChatLoopTest {

  @Test
  public void isExitRecognizesExitAndQuit() {
    assertTrue(ReviewChatLoop.isExit("exit"));
    assertTrue(ReviewChatLoop.isExit("QUIT"));
    assertFalse(ReviewChatLoop.isExit("explain the summary"));
  }

  @Test
  public void shouldEnableRespectsNoChatFlag() {
    assertFalse(ReviewChatLoop.shouldEnable(null, true));
    assertFalse(ReviewChatLoop.shouldEnable(true, true));
  }

  @Test
  public void shouldEnableHonoursExplicitChatFlag() {
    assertTrue(ReviewChatLoop.shouldEnable(true, false));
    assertFalse(ReviewChatLoop.shouldEnable(false, false));
  }

  @Test
  public void exitEndsChatWithoutExceptionalFailure() {
    final ReviewSessionContext session = new ReviewSessionContext(
        new AppConfig(
            new ModelConfig("ollama", "test", 0.2, "http://localhost:11434", 30, null),
            Path.of("rules"),
            8000,
            4096,
            512,
            256,
            0,
            List.of()
        ),
        List.of(),
        List.of(),
        Map.of(),
        "report"
    );

    final ByteArrayInputStream input =
        new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8));
    final var originalIn = System.in;
    try {
      System.setIn(input);
      assertTrue(ReviewChatLoop.run(session).wasNoError());
    } finally {
      System.setIn(originalIn);
    }
  }
}