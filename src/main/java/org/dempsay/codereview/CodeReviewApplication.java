package org.dempsay.codereview;

import org.dempsay.codereview.cli.CodeReviewCommand;
import picocli.CommandLine;

public final class CodeReviewApplication {

  private CodeReviewApplication() {
  }

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