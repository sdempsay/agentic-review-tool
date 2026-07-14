package org.dempsay.codereview.review;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class ReviewOutputValidatorTest {

  private static final String DIFF = """
      diff --git a/src/App.java b/src/App.java
      index 111..222 100644
      --- a/src/App.java
      +++ b/src/App.java
      @@ -10,4 +10,5 @@
       public class App {
      -    if(foo){
      +    if (foo) {
           return;
       }
      """;

  @Test
  public void validateAcceptsCleanVerdict() {
    final var result = ReviewOutputValidator.validate(
        "java-formatting",
        scopedFiles(),
        "## Clean"
    );
    assertTrue(result.valid());
  }

  @Test
  public void validateAcceptsValidFindingOnAddedLine() {
    final var result = ReviewOutputValidator.validate(
        "java-formatting",
        scopedFiles(),
        "- `src/App.java:11` — nit — missing brace style"
    );
    assertTrue(result.valid());
  }

  @Test
  public void validateRejectsReEvaluationNarrative() {
    final var result = ReviewOutputValidator.validate(
        "java-formatting",
        scopedFiles(),
        """
        Correction: false positive on spacing.
        ## Clean
        """
    );
    assertFalse(result.valid());
    assertTrue(result.violations().stream().anyMatch(v -> v.contains("re-evaluation")));
  }

  @Test
  public void validateRejectsVerboseCleanWithFindings() {
    final var result = ReviewOutputValidator.validate(
        "java-formatting",
        scopedFiles(),
        """
        - `src/App.java:11` — nit — spacing
        Clean: all 47 files in scope
        """
    );
    assertFalse(result.valid());
    assertTrue(result.violations().stream().anyMatch(v -> v.contains("enumerates clean")));
  }

  @Test
  public void validateAllowsCleanOtherFilesPhraseWithFindings() {
    final var result = ReviewOutputValidator.validate(
        "java-formatting",
        scopedFiles(),
        """
        - `src/App.java:11` — nit — spacing
        Clean: all other files in scope
        """
    );
    assertTrue(result.valid());
  }

  @Test
  public void validateRejectsCitationOnNonAddedLine() {
    final var result = ReviewOutputValidator.validate(
        "java-formatting",
        scopedFiles(),
        "- `src/App.java:10` — nit — cites context line only"
    );
    assertFalse(result.valid());
    assertTrue(result.violations().stream().anyMatch(v -> v.contains("not a + line")));
  }

  @Test
  public void validateRejectsOutOfScopePath() {
    final var result = ReviewOutputValidator.validate(
        "java-formatting",
        scopedFiles(),
        "- `other/Other.java:1` — nit — out of scope"
    );
    assertFalse(result.valid());
    assertTrue(result.violations().stream().anyMatch(v -> v.contains("out of scope")));
  }

  @Test
  public void validateRejectsMustFixForNitOnlyRuleset() {
    final var result = ReviewOutputValidator.validate(
        "java-javadoc",
        scopedFiles(),
        "- `src/App.java:11` — must-fix — bogus severity"
    );
    assertFalse(result.valid());
    assertTrue(result.violations().stream().anyMatch(v -> v.contains("must-fix not allowed")));
  }

  @Test
  public void validateAllowsMustFixForExceptionalRuleset() {
    final var result = ReviewOutputValidator.validate(
        "java-exceptional",
        scopedFiles(),
        "- `src/App.java:11` — must-fix — throws in supply lambda"
    );
    assertTrue(result.valid());
  }

  @Test
  public void validateRejectsWildcardImportClaimWithoutEvidence() {
    final var result = ReviewOutputValidator.validate(
        "java-formatting",
        scopedFiles(),
        "- `src/App.java:11` — nit — wildcard import on line"
    );
    assertFalse(result.valid());
    assertTrue(result.violations().stream().anyMatch(v -> v.contains("wildcard-import")));
  }

  @Test
  public void validateAcceptsWildcardImportClaimWithEvidence() {
    final String diff = """
        diff --git a/src/App.java b/src/App.java
        --- a/src/App.java
        +++ b/src/App.java
        @@ -1,2 +1,3 @@
         package app;
        +import java.util.*;
        """;

    final var result = ReviewOutputValidator.validate(
        "java-formatting",
        List.of(ChangedFile.included("src/App.java", ChangeType.MODIFIED, diff)),
        "- `src/App.java:2` — nit — wildcard import"
    );
    assertTrue(result.valid());
  }

  @Test
  public void validateRejectsBogusSpacingClaimWhenKeywordAlreadySpaced() {
    final var result = ReviewOutputValidator.validate(
        "java-formatting",
        scopedFiles(),
        "- `src/App.java:11` — nit — missing space before ("
    );
    assertFalse(result.valid());
    assertTrue(result.violations().stream().anyMatch(v -> v.contains("bogus spacing")));
  }

  @Test
  public void validateHandlesOpenRouterFormattingPreambleWithoutCrashing() {
    final String output = """
        Looking at the unified diffs, I'll review only the added (`+`) lines for Java formatting violations:

        - `src/main/java/org/dempsay/codereview/review/AgentReviewRequest.java:23` — nit — §1 — missing final newline
        - `src/test/java/org/dempsay/codereview/review/ReviewOutputValidatorTest.java:174` — nit — §1 — missing final newline
        """;

    ReviewOutputValidator.validate("java-formatting", scopedFiles(), output);
  }

  private static List<ChangedFile> scopedFiles() {
    return List.of(ChangedFile.included("src/App.java", ChangeType.MODIFIED, DIFF));
  }
}
