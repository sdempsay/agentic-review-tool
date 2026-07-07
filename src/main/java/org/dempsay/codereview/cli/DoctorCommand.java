package org.dempsay.codereview.cli;

import java.nio.file.Path;
import org.dempsay.codereview.config.ConfigLoader;
import org.dempsay.codereview.model.ModelHealthChecker;
import org.dempsay.codereview.support.FailureCapture;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "doctor",
    description = "Check configuration and LLM connectivity",
    mixinStandardHelpOptions = true
)
public class DoctorCommand implements Runnable {

  @Option(
      names = "--config",
      description = "Path to config.json (default: ~/.code-review/config.json, then bundled defaults)"
  )
  private Path configPath;

  @Override
  public void run() {
    final FailureCapture failures = new FailureCapture();
    final ExceptionalResponse<Boolean> outcome = ConfigLoader.load(configPath)
        .chain((listener, config) -> ModelHealthChecker.check(config.model())
            .chain((healthListener, report) -> {
              System.out.println("Config: " + ConfigLoader.describeSource(configPath));
              System.out.printf(
                  "OK: %s (%s @ %s)%n",
                  report.message(),
                  report.modelName(),
                  report.baseUrl()
              );
              return ExceptionalResponse.success(Boolean.TRUE);
            }, listener), failures.listener());
    failures.failIfError(outcome);
  }
}