package org.dempsay.codereview.review;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class ReviewReportComposerTest {

  @Test
  public void composeCombinesAgentReviewsAndSummary() {
    final String report = ReviewReportComposer.compose(
        List.of(new ReviewResult("java-general", "Fix indentation")),
        """
        ### Health Score
        8/10

        ### Recommendation
        APPROVE_WITH_NITS
        """
    );

    assertTrue(report.contains("--- Agent Reviews ---"));
    assertTrue(report.contains("=== Review: java-general ==="));
    assertTrue(report.contains("Fix indentation"));
    assertTrue(report.contains("--- Summary ---"));
    assertTrue(report.contains("8/10"));
    assertTrue(report.contains("APPROVE_WITH_NITS"));
  }
}