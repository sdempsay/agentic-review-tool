package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class ReviewAggregatorTest {

  @Test
  public void aggregateCombinesAgentFindingsWithHeaders() {
    final String output = ReviewAggregator.aggregate(
        List.of(
            new ReviewResult("java-general", "Indentation issue in App.java"),
            new ReviewResult("pom-tidy", "Dependency order looks good")
        )
    );

    assertTrue(output.contains("=== Review: java-general ==="));
    assertTrue(output.contains("Indentation issue in App.java"));
    assertTrue(output.contains("=== Review: pom-tidy ==="));
    assertTrue(output.contains("Dependency order looks good"));
  }

  @Test
  public void aggregateReturnsMessageWhenNoResults() {
    assertEquals("No reviewable diffs found.", ReviewAggregator.aggregate(List.of()));
  }
}