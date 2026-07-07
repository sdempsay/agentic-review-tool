package org.dempsay.codereview.cli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReviewProgressTest {

  @Test
  public void quietSuppressesStreaming() {
    final ReviewProgress progress = ReviewProgress.create(CliVerbosity.QUIET);
    assertTrue(progress.isQuiet());
    assertFalse(progress.shouldStreamLlm());
    assertFalse(progress.isVerbose());
  }

  @Test
  public void verboseEnablesThinkingStream() {
    final ReviewProgress progress = ReviewProgress.create(CliVerbosity.VERBOSE);
    assertTrue(progress.isVerbose());
    assertTrue(progress.shouldStreamLlm());
  }
}