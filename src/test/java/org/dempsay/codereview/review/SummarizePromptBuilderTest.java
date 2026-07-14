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
    assertTrue(prompt.contains("Diff summarization rules"));
    assertTrue(prompt.contains("final verdict is `## Clean`"));
    assertTrue(prompt.contains("Treat agent must-fix as invalid"));
    assertTrue(prompt.contains("java-exceptional and java-javadoc are Clean"));
  }

  @Test
  public void buildSanitizesRetractedAgentFindingsBeforeSummarize() {
    final String retracted = """
        - `App.java:10` — nit — invalid context line
        **Note**: **invalid** under diff discipline.
        Re-evaluating strictly on `+` lines:
        ## Clean
        """;
    final String prompt = SummarizePromptBuilder.build(
        List.of(new ReviewResult("java-formatting", retracted)),
        List.of(ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+line"))
    );

    assertTrue(prompt.contains("### java-formatting"));
    assertTrue(prompt.contains("## Clean"));
    assertTrue(prompt.indexOf("### java-formatting") < prompt.indexOf("## Clean"));
    assertFalse(prompt.contains("invalid context line"));
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

  @Test
  public void buildUsesRepositorySummarizeFixture() throws Exception {
    final String prompt = SummarizePromptBuilder.build(
        RepoSummarizeFixture.agentResults(),
        RepoSummarizeFixture.changedFiles(),
        ReviewContentMode.FULL_FILE
    );

    assertTrue(prompt.contains("repository review summarizer"));
    assertTrue(prompt.contains("## Repository Coverage"));
    assertTrue(prompt.contains("`src/main`: 4 reviewable files"));
    assertTrue(prompt.contains("`src/test`: 2 reviewable files"));
    assertTrue(prompt.contains("### java-general"));
    assertTrue(prompt.contains("duplicated orchestration"));
    assertTrue(prompt.contains("### Hotspot Areas"));
    assertTrue(prompt.contains("### Cross-Cutting Findings"));
    assertTrue(prompt.contains("overall codebase health"));
    assertTrue(prompt.contains("### Top Actions"));
    assertFalse(prompt.contains("overall change health"));
  }
}