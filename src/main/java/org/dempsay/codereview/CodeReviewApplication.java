package org.dempsay.codereview;

import org.dempsay.codereview.cli.CodeReviewCommand;
import picocli.CommandLine;

/**
 * Entry point for the code-review CLI.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class CodeReviewApplication {

  private CodeReviewApplication() {
  }

  /**
   * Runs the code-review CLI and exits with the command status code.
   * 
   * @param args the args
   * @since 1.0.0
 */
  public static void main(final String[] args) {
    final CommandLine commandLine = new CommandLine(new CodeReviewCommand());
    commandLine.setExecutionExceptionHandler((exception, cmd, parseResult) -> {
      cmd.getErr().println(cmd.getColorScheme().errorText(exception.getMessage()));
      return 1;
    });
    final int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }
}