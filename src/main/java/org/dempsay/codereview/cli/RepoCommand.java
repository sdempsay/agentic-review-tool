package org.dempsay.codereview.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.config.ConfigLoader;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.ingest.RepoIngestRequest;
import org.dempsay.codereview.ingest.RepoIngestService;
import org.dempsay.codereview.review.LlmReviewService;
import org.dempsay.codereview.review.ReviewSessionContext;
import org.dempsay.codereview.rules.Rule;
import org.dempsay.codereview.rules.RulesClassifier;
import org.dempsay.codereview.rules.RulesEngine;
import org.dempsay.codereview.support.FailureCapture;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "repo",
    description = "Review repository files (tracked + untracked, gitignore-safe)",
    mixinStandardHelpOptions = true
)
public class RepoCommand implements Runnable {

  @Option(
      names = "--config",
      description = "Path to config.json (default: ~/.code-review/config.json, then bundled defaults)"
  )
  private Path configPath;

  @Option(
      names = "--dry-run",
      description = "Classify repository files without calling the LLM"
  )
  private boolean dryRun;

  @Option(
      names = "--path",
      description = "Only include files matching this glob (repeatable)"
  )
  private List<String> pathGlobs;

  @Option(
      names = "--include-ext",
      description = "Only include files with these extensions (e.g. .java); overrides default .md/.json exclusions"
  )
  private List<String> includeExtensions;

  @Option(
      names = "--exclude-ext",
      description = "Additional extensions to exclude (e.g. .xml)"
  )
  private List<String> excludeExtensions;

  @Option(
      names = "--output",
      description = "Write the full report to this Markdown file path"
  )
  private Path outputPath;

  @Option(
      names = "--chat",
      description = "Enable follow-up chat after review (default when stdin is a TTY)"
  )
  private Boolean chat;

  @Option(
      names = "--no-chat",
      description = "Skip follow-up chat after review"
  )
  private boolean noChat;

  @Option(
      names = "--quiet",
      description = "Suppress progress output; print errors and final report only"
  )
  private boolean quiet;

  @Option(
      names = "--verbose",
      description = "Show detailed per-file progress and streamed model thinking"
  )
  private boolean verbose;

  @Override
  public void run() {
    final FailureCapture failures = new FailureCapture();
    final ExceptionalResponse<Boolean> outcome = dryRun ? runDryRun(failures) : runLlmReview(failures);
    failures.failIfError(outcome);
  }

  private ExceptionalResponse<Boolean> runLlmReview(final FailureCapture failures) {
    final ReviewProgress progress = ReviewProgress.create(CliVerbosity.fromFlags(quiet, verbose));
    return ConfigLoader.load(configPath)
        .chain((listener, config) -> RulesEngine.load(config.rulesDir(), listener)
            .chain((rulesListener, rules) -> {
              final long ingestStageStart = System.currentTimeMillis();
              progress.stageStart("Ingest");
              return RepoIngestService.ingest(buildIngestRequest(config))
                  .chain((ingestListener, changedFiles) -> {
                    progress.stageComplete("Ingest", ingestStageStart);
                    final long classifyStageStart = System.currentTimeMillis();
                    progress.stageStart("Classify");
                    final Map<String, List<Rule>> classification = classify(rules, changedFiles);
                    progress.stageComplete("Classify", classifyStageStart);

                    if (!progress.isQuiet()) {
                      IngestSummaryRenderer.render(changedFiles);
                      DryRunRenderer.render(classification);
                    }

                    return LlmReviewService.review(config, rules, changedFiles, progress)
                        .chain((reviewListener, reviewText) -> {
                          System.out.println();
                          System.out.println(reviewText);
                          return runChatIfEnabled(config, rules, changedFiles, classification, reviewText)
                              .chain((chatListener, ignored) ->
                                  writeReportIfRequested(reviewText, changedFiles, classification, ingestListener),
                                  reviewListener);
                        }, ingestListener);
                  }, rulesListener);
            }, listener), failures.listener());
  }

  private ExceptionalResponse<Boolean> runDryRun(final FailureCapture failures) {
    final ReviewProgress progress = ReviewProgress.create(CliVerbosity.fromFlags(quiet, verbose));
    return ConfigLoader.load(configPath)
        .chain((listener, config) -> RulesEngine.load(config.rulesDir(), listener)
            .chain((rulesListener, rules) -> {
              final long ingestStageStart = System.currentTimeMillis();
              progress.stageStart("Ingest");
              return RepoIngestService.ingest(buildIngestRequest(config))
                  .chain((ingestListener, changedFiles) -> {
                    progress.stageComplete("Ingest", ingestStageStart);
                    final long classifyStageStart = System.currentTimeMillis();
                    progress.stageStart("Classify");
                    final Map<String, List<Rule>> classification = classify(rules, changedFiles);
                    progress.stageComplete("Classify", classifyStageStart);
                    if (!progress.isQuiet()) {
                      DryRunRenderer.render(classification);
                    }
                    return writeReportIfRequested(null, changedFiles, classification, ingestListener);
                  }, rulesListener);
            }, listener), failures.listener());
  }

  private ExceptionalResponse<Boolean> runChatIfEnabled(
      final AppConfig config,
      final List<Rule> rules,
      final List<ChangedFile> changedFiles,
      final Map<String, List<Rule>> classification,
      final String reviewText
  ) {
    if (!ReviewChatLoop.shouldEnable(chat, noChat)) {
      return ExceptionalResponse.success(Boolean.TRUE);
    }

    try {
      ReviewChatLoop.run(new ReviewSessionContext(config, rules, changedFiles, classification, reviewText));
      return ExceptionalResponse.success(Boolean.TRUE);
    } catch (IOException exception) {
      throw new IllegalStateException("Follow-up chat failed: " + exception.getMessage(), exception);
    }
  }

  private ExceptionalResponse<Boolean> writeReportIfRequested(
      final String reviewText,
      final List<ChangedFile> changedFiles,
      final Map<String, List<Rule>> classification,
      final ExceptionalListener listener
  ) {
    if (outputPath == null) {
      return ExceptionalResponse.success(Boolean.TRUE);
    }

    final String markdown = MarkdownReportBuilder.build(
        describeScope(),
        changedFiles,
        classification,
        reviewText
    );
    return ReportExporter.write(outputPath, markdown, listener)
        .chain((writeListener, writtenPath) -> {
          System.out.println("Report written to: " + writtenPath);
          return ExceptionalResponse.success(Boolean.TRUE);
        }, listener);
  }

  private RepoIngestRequest buildIngestRequest(final AppConfig config) {
    return new RepoIngestRequest(
        Path.of("").toAbsolutePath(),
        config.maxDiffKb(),
        pathGlobs == null ? List.of() : pathGlobs,
        includeExtensions == null ? List.of() : includeExtensions,
        excludeExtensions == null ? List.of() : excludeExtensions
    );
  }

  private String describeScope() {
    return RepoScopeDescriber.describe(pathGlobs, includeExtensions, excludeExtensions);
  }

  private static Map<String, List<Rule>> classify(final List<Rule> rules, final List<ChangedFile> changedFiles) {
    final List<String> filePaths = changedFiles.stream().map(ChangedFile::path).toList();
    return RulesClassifier.classify(rules, filePaths);
  }
}