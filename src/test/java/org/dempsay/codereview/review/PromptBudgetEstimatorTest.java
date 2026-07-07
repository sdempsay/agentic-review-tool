package org.dempsay.codereview.review;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PromptBudgetEstimatorTest {

  @Test
  public void diffBudgetBytesReservesGenerationAndOverhead() {
    final int budget = PromptBudgetEstimator.diffBudgetBytes(8192, 8000, 500);

    assertTrue(budget < 500);
  }

  @Test
  public void diffBudgetBytesScalesWithLargeContext() {
    final int budget = PromptBudgetEstimator.diffBudgetBytes(262144, 8000, 2000);

    assertTrue(budget > 500_000);
  }
}