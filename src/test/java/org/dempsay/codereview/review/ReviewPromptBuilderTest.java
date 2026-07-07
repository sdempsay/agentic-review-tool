package org.dempsay.codereview.review;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class ReviewPromptBuilderTest {

  @Test
  public void buildPromptIncludesDiffContent() {
    final String prompt = ReviewPromptBuilder.build(
        List.of(
            ChangedFile.included(
                "src/App.java",
                ChangeType.MODIFIED,
                "diff --git a/src/App.java b/src/App.java\n+new line"
            ),
            ChangedFile.skipped("large.bin", ChangeType.ADDED, "Binary file")
        )
    );

    assertTrue(prompt.contains("src/App.java"));
    assertTrue(prompt.contains("+new line"));
    assertTrue(prompt.contains("large.bin"));
    assertTrue(prompt.contains("Binary file"));
  }

  @Test
  public void buildPromptHandlesNoReviewableDiffs() {
    final String response = LlmReviewService.reviewRequired(
        new org.dempsay.codereview.config.AppConfig(
            new org.dempsay.codereview.config.ModelConfig("ollama", "qwen3", 0.2, null, 0),
            java.nio.file.Path.of("/tmp"),
            8000,
            512
        ),
        List.of(ChangedFile.skipped("image.png", ChangeType.ADDED, "Binary file"))
    );

    assertTrue(response.contains("No reviewable diffs found"));
  }
}