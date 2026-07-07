package org.dempsay.codereview.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "code-review",
    description = "Cost-effective agentic code review pipeline",
    subcommands = {DiffCommand.class, RepoCommand.class, DoctorCommand.class},
    mixinStandardHelpOptions = true
)
public class CodeReviewCommand implements Runnable {

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }
}