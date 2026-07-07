package org.dempsay.codereview.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.config.ConfigLoader;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.ingest.GitIngestService;
import org.dempsay.codereview.ingest.IngestRequest;
import org.dempsay.codereview.review.LlmReviewService;
import org.dempsay.codereview.rules.Rule;
import org.dempsay.codereview.rules.RulesClassifier;
import org.dempsay.codereview.rules.RulesEngine;
import org.dempsay.codereview.support.FailureCapture;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "diff",
    description = "Review uncommitted changes (staged + unstaged)",
    mixinStandardHelpOptions = true
)
public class DiffCommand implements Runnable {

  @Option(
      names = "--config",
      description = "Path to config.json (default: ~/.code-review/config.json, then bundled defaults)"
  )
  private Path configPath;

  @Option(
      names = "--dry-run",
      description = "Classify changed files against loaded rules without calling the LLM"
  )
  private boolean dryRun;

  @Option(
      names = "--staged",
      description = "Review staged changes only (git diff --cached)"
  )
  private boolean staged;

  @Option(
      names = "--base",
      description = "Review changes against a base ref (git diff <base>...HEAD)"
  )
  private String baseRef;

  @Override
  public void run() {
    final FailureCapture failures = new FailureCapture();
    final ExceptionalResponse<Boolean> outcome = dryRun ? runDryRun(failures) : runLlmReview(failures);
    failures.failIfError(outcome);
  }

  private ExceptionalResponse<Boolean> runLlmReview(final FailureCapture failures) {
    return ConfigLoader.load(configPath)
        .chain((listener, config) -> RulesEngine.load(config.rulesDir(), listener)
            .chain((rulesListener, rules) -> GitIngestService.ingest(buildIngestRequest(config))
                .chain((ingestListener, changedFiles) -> {
                  IngestSummaryRenderer.render(changedFiles);
                  renderClassification(rules, changedFiles);
                  return LlmReviewService.review(config, rules, changedFiles)
                      .chain((reviewListener, reviewText) -> {
                        System.out.println();
                        System.out.println("--- LLM Review ---");
                        System.out.println(reviewText);
                        return ExceptionalResponse.success(Boolean.TRUE);
                      }, ingestListener);
                }, rulesListener), listener), failures.listener());
  }

  private ExceptionalResponse<Boolean> runDryRun(final FailureCapture failures) {
    return ConfigLoader.load(configPath)
        .chain((listener, config) -> RulesEngine.load(config.rulesDir(), listener)
            .chain((rulesListener, rules) -> GitIngestService.ingest(buildIngestRequest(config))
                .chain((ingestListener, changedFiles) -> {
                  renderClassification(rules, changedFiles);
                  return ExceptionalResponse.success(Boolean.TRUE);
                }, rulesListener), listener), failures.listener());
  }

  private IngestRequest buildIngestRequest(final AppConfig config) {
    final Path repoRoot = Path.of("").toAbsolutePath();
    if (staged) {
      return IngestRequest.staged(repoRoot, config.maxDiffKb());
    }
    if (baseRef != null && !baseRef.isBlank()) {
      return IngestRequest.againstBase(repoRoot, baseRef, config.maxDiffKb());
    }
    return IngestRequest.uncommitted(repoRoot, config.maxDiffKb());
  }

  private static void renderClassification(final List<Rule> rules, final List<ChangedFile> changedFiles) {
    final List<String> filePaths = changedFiles.stream().map(ChangedFile::path).toList();
    final Map<String, List<Rule>> matches = RulesClassifier.classify(rules, filePaths);
    DryRunRenderer.render(matches);
  }
}