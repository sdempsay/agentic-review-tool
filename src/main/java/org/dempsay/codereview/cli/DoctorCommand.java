package org.dempsay.codereview.cli;

import java.nio.file.Path;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.config.ConfigLoader;
import org.dempsay.codereview.model.ModelHealthChecker;
import org.dempsay.codereview.model.OllamaModelInspector;
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
    final ExceptionalResponse<Boolean> outcome = ConfigLoader.load(configPath, failures.listener())
        .chain((listener, config) -> ModelHealthChecker.check(config.model(), listener)
            .chain((healthListener, report) -> {
              System.out.println("Config: " + ConfigLoader.describeSource(configPath));
              System.out.printf(
                  "OK: %s (%s @ %s)%n",
                  report.message(),
                  report.modelName(),
                  report.baseUrl()
              );
              printBatchCapInfo(config);
              return ExceptionalResponse.success(Boolean.TRUE);
            }, listener), failures.listener());
    failures.failIfError(outcome);
  }

  private static void printBatchCapInfo(final AppConfig config) {
    if (config.model().isOpenRouter()) {
      System.out.printf(
          "Batch caps: maxAgentDiffKb=%d (soft and hard; OpenRouter has no num_ctx probe)%n",
          config.maxAgentDiffKb()
      );
      return;
    }

    final int contextTokens = OllamaModelInspector.resolveContextTokens(config.model());
    if (contextTokens > 0) {
      System.out.printf(
          "Batch caps: num_ctx=%d tokens (hard), maxAgentDiffKb=%d (soft)%n",
          contextTokens,
          config.maxAgentDiffKb()
      );
      return;
    }
    if (config.maxAgentDiffKb() > 0) {
      System.out.printf(
          "Batch caps: maxAgentDiffKb=%d (soft and hard; num_ctx unavailable)%n",
          config.maxAgentDiffKb()
      );
    }
  }
}