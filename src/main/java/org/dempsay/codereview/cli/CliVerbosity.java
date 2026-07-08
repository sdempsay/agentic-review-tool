package org.dempsay.codereview.cli;

/**
 * CLI output verbosity level.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public enum CliVerbosity {
  QUIET,
  NORMAL,
  VERBOSE;

  /**
   * Resolves verbosity from CLI flags.
   * 
   * @param quiet the quiet
   * @param verbose the verbose
   * @return the result
   * @since 1.0.0
 */
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