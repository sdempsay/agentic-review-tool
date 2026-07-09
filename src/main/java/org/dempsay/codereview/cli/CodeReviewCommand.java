package org.dempsay.codereview.cli;

import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "code-review",
    description = "Cost-effective agentic code review pipeline",
    subcommands = {
        DiffCommand.class,
        RepoCommand.class,
        DoctorCommand.class,
        GenerateCompletion.class
    },
    mixinStandardHelpOptions = true
)
/**
 * Root picocli command for the code-review CLI.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public class CodeReviewCommand implements Runnable {

  @Override
  /**
   * Executes this command.
   * 
   * @since 1.0.0
 */
  public void run() {
    CommandLine.usage(this, System.out);
  }
}