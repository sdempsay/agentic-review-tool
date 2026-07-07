package org.dempsay.codereview.cli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReviewChatLoopTest {

  @Test
  public void isExitRecognizesExitAndQuit() {
    assertTrue(ReviewChatLoop.isExit("exit"));
    assertTrue(ReviewChatLoop.isExit("QUIT"));
    assertFalse(ReviewChatLoop.isExit("explain the summary"));
  }

  @Test
  public void shouldEnableRespectsNoChatFlag() {
    assertFalse(ReviewChatLoop.shouldEnable(null, true));
    assertFalse(ReviewChatLoop.shouldEnable(true, true));
  }

  @Test
  public void shouldEnableHonoursExplicitChatFlag() {
    assertTrue(ReviewChatLoop.shouldEnable(true, false));
    assertFalse(ReviewChatLoop.shouldEnable(false, false));
  }
}