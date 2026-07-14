package org.dempsay.codereview.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.config.ConfigLoader;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.ingest.DiffIngestService;
import org.dempsay.codereview.ingest.GitIngestService;
import org.dempsay.codereview.ingest.IngestRequest;
import org.dempsay.codereview.review.LlmReviewService;
import org.dempsay.codereview.review.ReviewSessionContext;
import org.dempsay.codereview.rules.Rule;
import org.dempsay.codereview.rules.RulesClassifier;
import org.dempsay.codereview.rules.RulesEngine;
import org.dempsay.codereview.support.FailureCapture;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
    name = "diff",
    description = "Review git changes, or a unified diff from --stdin / --diff-file (no git required)",
    mixinStandardHelpOptions = true
)
/**
 * Reviews uncommitted, staged, or base-ref git changes.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public class DiffCommand implements Runnable {

  @Spec
  private CommandLine.Model.CommandSpec spec;

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

  @Option(
      names = "--stdin",
      description = "Read unified diff from stdin (for piping; chat is unavailable with --stdin)"
  )
  private boolean stdin;

  @Option(
      names = "--diff-file",
      description = "Read unified diff from file (repeatable; no git required)"
  )
  private List<Path> diffFiles;

  @Option(
      names = "--output",
      description = "Write the full report to this Markdown file path"
  )
  private Path outputPath;

  @Option(
      names = "--chat",
      description = "Enable follow-up chat after review (off by default)"
  )
  private Boolean chat;

  @Option(
      names = "--no-chat",
      description = "Skip follow-up chat after review (default)"
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
  /**
   * Executes this command.
   * 
   * @since 1.0.0
 */
  public void run() {
    validateInputMode();
    final FailureCapture failures = new FailureCapture();
    final ExceptionalResponse<Boolean> outcome = dryRun ? runDryRun(failures) : runLlmReview(failures);
    failures.failIfError(outcome);
  }

  private void validateInputMode() {
    final boolean externalDiff = stdin || hasDiffFiles();
    if (!externalDiff) {
      return;
    }
    if (staged || baseRef != null && !baseRef.isBlank()) {
      throw new CommandLine.ParameterException(
          spec.commandLine(),
          "--stdin/--diff-file cannot be combined with --staged or --base"
      );
    }
    if (Boolean.TRUE.equals(chat)) {
      throw new CommandLine.ParameterException(
          spec.commandLine(),
          "Follow-up chat cannot read stdin when --stdin supplies the diff; use --no-chat"
      );
    }
  }

  private ExceptionalResponse<Boolean> runLlmReview(final FailureCapture failures) {
    final ReviewProgress progress = ReviewProgress.create(CliVerbosity.fromFlags(quiet, verbose));
    return ConfigLoader.load(configPath, failures.listener())
        .chain((listener, config) -> RulesEngine.load(config.rulesDir(), listener)
            .chain((rulesListener, rules) -> {
              final long ingestStageStart = System.currentTimeMillis();
              progress.stageStart("Ingest");
              return ingestChangedFiles(config, rulesListener)
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

                    return LlmReviewService.review(config, rules, changedFiles, progress, ingestListener)
                        .chain((reviewListener, reviewText) -> {
                          System.out.println();
                          System.out.println(reviewText);
                          return runChatIfEnabled(
                                  config, rules, changedFiles, classification, reviewText, reviewListener)
                              .chain((chatListener, ignored) ->
                                  writeReportIfRequested(reviewText, changedFiles, classification, ingestListener),
                                  reviewListener);
                        }, ingestListener);
                  }, rulesListener);
            }, listener), failures.listener());
  }

  private ExceptionalResponse<Boolean> runDryRun(final FailureCapture failures) {
    final ReviewProgress progress = ReviewProgress.create(CliVerbosity.fromFlags(quiet, verbose));
    return ConfigLoader.load(configPath, failures.listener())
        .chain((listener, config) -> RulesEngine.load(config.rulesDir(), listener)
            .chain((rulesListener, rules) -> {
              final long ingestStageStart = System.currentTimeMillis();
              progress.stageStart("Ingest");
              return ingestChangedFiles(config, rulesListener)
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
      final String reviewText,
      final ExceptionalListener listener
  ) {
    if (!chatEnabled()) {
      return ExceptionalResponse.success(Boolean.TRUE);
    }

    return ReviewChatLoop.run(
            new ReviewSessionContext(config, rules, changedFiles, classification, reviewText),
            listener
        )
        .chain((chatListener, ignored) -> ExceptionalResponse.success(Boolean.TRUE), listener);
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

    final String markdown = MarkdownReportBuilder.build(describeScope(), changedFiles, classification, reviewText);
    return ReportExporter.write(outputPath, markdown, listener)
        .chain((writeListener, writtenPath) -> {
          System.out.println("Report written to: " + writtenPath);
          return ExceptionalResponse.success(Boolean.TRUE);
        }, listener);
  }

  private ExceptionalResponse<List<ChangedFile>> ingestChangedFiles(
      final AppConfig config,
      final ExceptionalListener listener
  ) {
    if (stdin || hasDiffFiles()) {
      return DiffIngestService.ingestExternal(stdin, diffFiles, config.maxDiffKb(), listener);
    }
    return GitIngestService.ingest(buildIngestRequest(config), listener);
  }

  private boolean hasDiffFiles() {
    return diffFiles != null && !diffFiles.isEmpty();
  }

  private boolean chatEnabled() {
    if (stdin || hasDiffFiles()) {
      return false;
    }
    return ReviewChatLoop.shouldEnable(chat, noChat);
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

  private String describeScope() {
    if (stdin && hasDiffFiles()) {
      return "provided diff (stdin + " + diffFiles.size() + " file(s))";
    }
    if (stdin) {
      return "provided diff (stdin)";
    }
    if (hasDiffFiles()) {
      return "provided diff (" + diffFiles.size() + " file(s))";
    }
    if (staged) {
      return "staged changes";
    }
    if (baseRef != null && !baseRef.isBlank()) {
      return "changes against " + baseRef;
    }
    return "uncommitted changes";
  }

  private static Map<String, List<Rule>> classify(final List<Rule> rules, final List<ChangedFile> changedFiles) {
    final List<String> filePaths = changedFiles.stream().map(ChangedFile::path).toList();
    return RulesClassifier.classify(rules, filePaths);
  }
}