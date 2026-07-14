package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.dempsay.codereview.cli.CliVerbosity;
import org.dempsay.codereview.cli.ReviewProgress;
import org.dempsay.codereview.config.ModelConfig;
import org.junit.Assert;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class GuardedLlmCompletionTest {

  private static final String DIFF = """
      diff --git a/src/App.java b/src/App.java
      --- a/src/App.java
      +++ b/src/App.java
      @@ -10,4 +10,5 @@
       public class App {
      +    if (foo) {
           return;
       }
      """;

  @Test
  public void completeAgentReviewRetriesOnceOnValidationFailure() {
    final AtomicInteger calls = new AtomicInteger();
    final List<String> prompts = new ArrayList<>();
    final String valid = "- `src/App.java:11` — nit — spacing";

    final AgentReviewRequest request = new AgentReviewRequest(
        new ModelConfig("ollama", "qwen3", 0.2, null, 0, null),
        512,
        "review prompt",
        ReviewProgress.create(CliVerbosity.VERBOSE),
        "java-formatting",
        "java-formatting",
        List.of(ChangedFile.included("src/App.java", ChangeType.MODIFIED, DIFF))
    );
    final String response = GuardedLlmCompletion.completeAgentReview(
        request,
        (model, maxTokens, prompt, progress, label) -> {
          calls.incrementAndGet();
          prompts.add(prompt);
          return calls.get() == 1 ? "Correction: re-evaluating" : valid;
        }
    );

    assertEquals(2, calls.get());
    assertEquals(valid, response);
    assertTrue(prompts.get(1).contains("FORMAT CORRECTION"));
    assertTrue(prompts.get(1).contains("re-evaluation narrative"));
  }

  @Test
  public void completeAgentReviewSkipsRetryWhenFirstResponseValid() {
    final AtomicInteger calls = new AtomicInteger();
    final String valid = "## Clean";

    final AgentReviewRequest request = new AgentReviewRequest(
        new ModelConfig("ollama", "qwen3", 0.2, null, 0, null),
        512,
        "review prompt",
        ReviewProgress.create(CliVerbosity.QUIET),
        "java-formatting",
        "java-formatting",
        List.of(ChangedFile.included("src/App.java", ChangeType.MODIFIED, DIFF))
    );
    final String response = GuardedLlmCompletion.completeAgentReview(
        request,
        (model, maxTokens, prompt, progress, label) -> {
          calls.incrementAndGet();
          return valid;
        }
    );

    assertEquals(1, calls.get());
    assertEquals(valid, response);
  }

  private static void assertTrue(final boolean condition) {
    Assert.assertTrue(condition);
  }
}
