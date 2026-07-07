package org.dempsay.codereview.review;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class SummarizePromptBuilderTest {

  @Test
  public void buildIncludesAgentFindingsAndRequiredFormat() {
    final String prompt = SummarizePromptBuilder.build(
        List.of(
            new ReviewResult("java-general", "Indentation issue in App.java"),
            new ReviewResult("pom-tidy", "Dependencies are ordered correctly")
        ),
        List.of(
            ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+line"),
            ChangedFile.skipped("large.bin", ChangeType.ADDED, "Binary file")
        )
    );

    assertTrue(prompt.contains("code review summarizer"));
    assertTrue(prompt.contains("Reviewable diffs: 1"));
    assertTrue(prompt.contains("Skipped: 1"));
    assertTrue(prompt.contains("### java-general"));
    assertTrue(prompt.contains("Indentation issue in App.java"));
    assertTrue(prompt.contains("### pom-tidy"));
    assertTrue(prompt.contains("### Health Score"));
    assertTrue(prompt.contains("APPROVE_WITH_NITS"));
    assertTrue(prompt.contains("### Top Actions"));
  }

  @Test
  public void buildUsesRepositoryStatsForFullFileMode() {
    final String prompt = SummarizePromptBuilder.build(
        List.of(new ReviewResult("java-general", "Looks good")),
        List.of(ChangedFile.included("src/App.java", ChangeType.EXISTING, "class App {}")),
        ReviewContentMode.FULL_FILE
    );

    assertTrue(prompt.contains("Files in scope: 1"));
    assertTrue(prompt.contains("Reviewable files: 1"));
    assertFalse(prompt.contains("Reviewable diffs"));
  }
}