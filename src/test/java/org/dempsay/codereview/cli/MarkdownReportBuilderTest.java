package org.dempsay.codereview.cli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;
import org.junit.Test;

public class MarkdownReportBuilderTest {

  @Test
  public void buildIncludesIngestClassificationAndReviewSections() {
    final Rule javaRule = new Rule("java-general", Path.of("java-general.md"), List.of("**/*.java"), "rules");
    final String markdown = MarkdownReportBuilder.build(
        "changes against HEAD^",
        List.of(ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+line")),
        Map.of("src/App.java", List.of(javaRule)),
        """
        --- Agent Reviews ---
        === Review: java-general ===
        Fix indentation

        --- Summary ---
        ### Health Score
        8/10
        """
    );

    assertTrue(markdown.contains("# Code Review Report"));
    assertTrue(markdown.contains("**Scope:** changes against HEAD^"));
    assertTrue(markdown.contains("## Ingest"));
    assertTrue(markdown.contains("`src/App.java`"));
    assertTrue(markdown.contains("## Classification"));
    assertTrue(markdown.contains("java-general"));
    assertTrue(markdown.contains("## Review"));
    assertTrue(markdown.contains("### Agent Reviews"));
    assertTrue(markdown.contains("### Review: java-general"));
    assertTrue(markdown.contains("### Summary"));
    assertTrue(markdown.contains("8/10"));
    assertFalse(markdown.contains("=== Review:"));
  }

  @Test
  public void buildOmitsReviewSectionForDryRun() {
    final String markdown = MarkdownReportBuilder.build(
        "staged changes",
        List.of(ChangedFile.included("README.md", ChangeType.MODIFIED, "+docs")),
        Map.of("README.md", List.of()),
        null
    );

    assertTrue(markdown.contains("## Ingest"));
    assertTrue(markdown.contains("## Classification"));
    assertFalse(markdown.contains("## Review"));
  }
}