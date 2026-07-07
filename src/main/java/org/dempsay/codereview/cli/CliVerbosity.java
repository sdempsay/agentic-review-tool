package org.dempsay.codereview.cli;

public enum CliVerbosity {
  QUIET,
  NORMAL,
  VERBOSE;

  public static CliVerbosity fromFlags(final boolean quiet, final boolean verbose) {
    if (quiet) {
      return QUIET;
    }
    if (verbose) {
      return VERBOSE;
    }
    return NORMAL;
  }
}