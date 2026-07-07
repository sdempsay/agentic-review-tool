package org.dempsay.codereview.cli;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CliVerbosityTest {

  @Test
  public void fromFlagsPrefersQuietOverVerbose() {
    assertEquals(CliVerbosity.QUIET, CliVerbosity.fromFlags(true, true));
    assertEquals(CliVerbosity.VERBOSE, CliVerbosity.fromFlags(false, true));
    assertEquals(CliVerbosity.NORMAL, CliVerbosity.fromFlags(false, false));
  }
}