package org.dempsay.codereview.review;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;
import org.junit.Test;

public class ReviewPromptBuilderTest {

  @Test
  public void buildPromptIncludesDiffContent() {
    final String prompt = ReviewPromptBuilder.build(
        Map.of(),
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
  public void buildPromptIncludesMatchedRulesOncePerSection() {
    final Rule javaRule = new Rule(
        "java-general",
        Path.of("/rules/java-general.md"),
        List.of("**/*.java"),
        "Check Java indentation and naming."
    );
    final String prompt = ReviewPromptBuilder.build(
        Map.of("src/App.java", List.of(javaRule)),
        List.of(
            ChangedFile.included(
                "src/App.java",
                ChangeType.MODIFIED,
                "diff --git a/src/App.java b/src/App.java\n+new line"
            )
        )
    );

    assertTrue(prompt.contains("## Review Rules"));
    assertTrue(prompt.contains("### Rule: java-general"));
    assertTrue(prompt.contains("Check Java indentation and naming."));
    assertTrue(prompt.contains("Applicable rules: java-general"));
    assertTrue(prompt.contains("+new line"));
  }

  @Test
  public void buildForRulesetIncludesOnlyRulesetInstructionsAndScopedFiles() {
    final Rule javaRule = new Rule(
        "java-general",
        Path.of("/rules/java-general.md"),
        List.of("**/*.java"),
        "Check Java indentation and naming."
    );
    final String prompt = ReviewPromptBuilder.buildForRuleset(
        javaRule,
        List.of(
            ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+new line"),
            ChangedFile.skipped("pom.xml", ChangeType.MODIFIED, "not in scope")
        )
    );

    assertTrue(prompt.contains("specialized code review agent for the \"java-general\" ruleset"));
    assertTrue(prompt.contains("Check Java indentation and naming."));
    assertTrue(prompt.contains("src/App.java"));
    assertTrue(prompt.contains("+new line"));
    assertTrue(prompt.contains("pom.xml"));
    assertTrue(prompt.contains("not in scope"));
  }

  @Test
  public void buildFollowUpIncludesRulesQuestionAndDiff() {
    final Rule javaRule = new Rule(
        "java-general",
        Path.of("java-general.md"),
        List.of("**/*.java"),
        "Check indentation."
    );
    final String prompt = ReviewPromptBuilder.buildFollowUp(
        javaRule,
        ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+line"),
        "Is the indentation really wrong?",
        "Prior finding: indentation issue"
    );

    assertTrue(prompt.contains("java-general"));
    assertTrue(prompt.contains("Check indentation."));
    assertTrue(prompt.contains("Is the indentation really wrong?"));
    assertTrue(prompt.contains("Prior finding: indentation issue"));
    assertTrue(prompt.contains("+line"));
  }

  @Test
  public void buildGeneralFallbackUsesGenericInstructions() {
    final String prompt = ReviewPromptBuilder.buildGeneralFallback(
        List.of(ChangedFile.included("README.md", ChangeType.MODIFIED, "+docs"))
    );

    assertTrue(prompt.contains("general code review agent"));
    assertTrue(prompt.contains("No specialized rules matched these files"));
    assertTrue(prompt.contains("README.md"));
    assertTrue(prompt.contains("+docs"));
  }

  @Test
  public void buildPromptDeduplicatesRulesAcrossFiles() {
    final Rule javaRule = new Rule(
        "java-general",
        null,
        List.of("**/*.java"),
        "Java style rules."
    );
    final String prompt = ReviewPromptBuilder.build(
        Map.of(
            "src/A.java", List.of(javaRule),
            "src/B.java", List.of(javaRule)
        ),
        List.of(
            ChangedFile.included("src/A.java", ChangeType.MODIFIED, "+a"),
            ChangedFile.included("src/B.java", ChangeType.MODIFIED, "+b")
        )
    );

    assertTrue(prompt.contains("### Rule: java-general"));
    assertFalse(prompt.contains("### Rule: java-general" + System.lineSeparator() + "Java style rules."
        + System.lineSeparator() + System.lineSeparator() + "### Rule: java-general"));
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
        List.of(),
        List.of(ChangedFile.skipped("image.png", ChangeType.ADDED, "Binary file")),
        org.dempsay.codereview.cli.ReviewProgress.create(org.dempsay.codereview.cli.CliVerbosity.QUIET)
    );

    assertTrue(response.contains("No reviewable diffs found"));
  }
}