package org.dempsay.codereview.review;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;
import org.dempsay.codereview.support.ExceptionalSupport;
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
    assertTrue(prompt.contains("```diff"));
    assertTrue(prompt.contains("pom.xml"));
    assertTrue(prompt.contains("not in scope"));
  }

  @Test
  public void buildForRulesetAppendsOutputFormatFromFile() {
    final Rule javaRule = new Rule(
        "java-formatting",
        Path.of("/rules/java-formatting.md"),
        List.of("**/*.java"),
        "Check Java indentation."
    );
    final ReviewPromptSupplements supplements = ExceptionalSupport.response(ReviewPromptSupplements.load(null));
    final String prompt = ReviewPromptBuilder.buildForRuleset(
        javaRule,
        List.of(ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+line")),
        ReviewContentMode.DIFF,
        supplements
    );

    assertTrue(prompt.contains("## Guardrails"));
    assertTrue(prompt.contains("no-modify"));
    assertTrue(prompt.contains("## Diff review discipline"));
    assertTrue(prompt.contains("## Output"));
    assertTrue(prompt.contains("insufficient context"));
    assertTrue(prompt.indexOf("## Guardrails") < prompt.indexOf("## Ruleset Instructions"));
    assertTrue(prompt.indexOf("## Diff review discipline") < prompt.indexOf("## Changed Files"));
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
    final String response = org.dempsay.codereview.support.ExceptionalSupport.response(
        LlmReviewService.review(
            new org.dempsay.codereview.config.AppConfig(
                new org.dempsay.codereview.config.ModelConfig("ollama", "qwen3", 0.2, null, 0, null),
                java.nio.file.Path.of("/tmp"),
                8000,
                512,
                256,
                0,
                List.of()
            ),
            List.of(),
            List.of(ChangedFile.skipped("image.png", ChangeType.ADDED, "Binary file")),
            org.dempsay.codereview.cli.ReviewProgress.create(org.dempsay.codereview.cli.CliVerbosity.QUIET)
        )
    );

    assertTrue(response.contains("No reviewable files found"));
  }

  @Test
  public void buildForRulesetFullFileModeUsesRepositorySectionAndFencedContent() {
    final Rule javaRule = new Rule(
        "java-general",
        Path.of("java-general.md"),
        List.of("**/*.java"),
        "Check Java style."
    );
    final String prompt = ReviewPromptBuilder.buildForRuleset(
        javaRule,
        List.of(ChangedFile.included("src/App.java", ChangeType.EXISTING, "class App {}")),
        ReviewContentMode.FULL_FILE
    );

    assertTrue(prompt.contains("full file contents"));
    assertTrue(prompt.contains("## Repository Files"));
    assertTrue(prompt.contains("```"));
    assertTrue(prompt.contains("class App {}"));
    assertFalse(prompt.contains("## Changed Files"));
  }

  @Test
  public void buildGeneralFallbackFullFileModeUsesRepositoryWording() {
    final String prompt = ReviewPromptBuilder.buildGeneralFallback(
        List.of(ChangedFile.included("pom.xml", ChangeType.EXISTING, "<project/>")),
        ReviewContentMode.FULL_FILE
    );

    assertTrue(prompt.contains("full file contents"));
    assertTrue(prompt.contains("## Repository Files"));
    assertTrue(prompt.contains("<project/>"));
  }
}