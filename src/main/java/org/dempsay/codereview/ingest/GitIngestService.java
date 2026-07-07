package org.dempsay.codereview.ingest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResource;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class GitIngestService {

  private static final String DEV_NULL = "/dev/null";

  private GitIngestService() {
  }

  public static ExceptionalResponse<List<ChangedFile>> ingest(final IngestRequest request) {
    if (!GitRunner.isGitRepository(request.repoRoot())) {
      return ExceptionalSupport.supply(() -> {
        throw new IllegalArgumentException("Not a git repository: " + request.repoRoot().toAbsolutePath());
      });
    }

    return switch (request.scope()) {
      case UNCOMMITTED -> ingestUncommitted(request);
      case STAGED -> ingestFromDiff(request, "diff", "--cached");
      case BASE -> ingestFromDiff(request, "diff", request.baseRef() + "...HEAD");
    };
  }

  private static ExceptionalResponse<List<ChangedFile>> ingestUncommitted(final IngestRequest request) {
    final Map<String, ChangedFile> files = new LinkedHashMap<>();
    final int maxDiffBytes = request.maxDiffKb() * 1024;

    return GitRunner.hasCommits(request.repoRoot())
        .chain((listener, hasCommits) -> {
          if (!hasCommits) {
            return appendUntrackedFiles(request.repoRoot(), files, maxDiffBytes, listener);
          }
          return GitRunner.run(request.repoRoot(), "diff", "HEAD")
              .chain((diffListener, result) -> {
                addParsedDiff(files, result.output(), maxDiffBytes);
                return appendUntrackedFiles(request.repoRoot(), files, maxDiffBytes, listener);
              }, listener);
        });
  }

  private static ExceptionalResponse<List<ChangedFile>> appendUntrackedFiles(
      final Path repoRoot,
      final Map<String, ChangedFile> files,
      final int maxDiffBytes,
      final ExceptionalListener listener
  ) {
    return GitRunner.runLines(repoRoot, "ls-files", "--others", "--exclude-standard")
        .chain((linesListener, untracked) ->
            processUntrackedPaths(repoRoot, untracked, files, maxDiffBytes, 0, listener),
            listener);
  }

  private static ExceptionalResponse<List<ChangedFile>> processUntrackedPaths(
      final Path repoRoot,
      final List<String> untracked,
      final Map<String, ChangedFile> files,
      final int maxDiffBytes,
      final int index,
      final ExceptionalListener listener
  ) {
    if (index >= untracked.size()) {
      return ExceptionalResponse.success(List.copyOf(files.values()));
    }

    final String path = untracked.get(index);
    if (files.containsKey(path)) {
      return processUntrackedPaths(repoRoot, untracked, files, maxDiffBytes, index + 1, listener);
    }

    return ingestUntrackedFile(repoRoot, path, maxDiffBytes)
        .chain((fileListener, changedFile) -> {
          files.put(path, changedFile);
          return processUntrackedPaths(repoRoot, untracked, files, maxDiffBytes, index + 1, listener);
        }, listener);
  }

  private static ExceptionalResponse<List<ChangedFile>> ingestFromDiff(
      final IngestRequest request,
      final String... gitDiffCommand
  ) {
    return GitRunner.run(request.repoRoot(), gitDiffCommand)
        .chain((listener, result) -> {
          if (result.exitCode() != 0) {
            throw new IllegalStateException(
                "git " + String.join(" ", gitDiffCommand) + " failed with exit code " + result.exitCode()
            );
          }

          final Map<String, ChangedFile> files = new LinkedHashMap<>();
          addParsedDiff(files, result.output(), request.maxDiffKb() * 1024);
          return ExceptionalResponse.success(List.copyOf(files.values()));
        });
  }

  private static void addParsedDiff(
      final Map<String, ChangedFile> files,
      final String rawDiff,
      final int maxDiffBytes
  ) {
    for (final DiffParser.ParsedDiffEntry entry : DiffParser.parse(rawDiff)) {
      files.put(entry.path(), toChangedFile(entry, maxDiffBytes));
    }
  }

  private static ExceptionalResponse<ChangedFile> ingestUntrackedFile(
      final Path repoRoot,
      final String path,
      final int maxDiffBytes
  ) {
    final Path filePath = repoRoot.resolve(path);
    if (!Files.isRegularFile(filePath)) {
      return ExceptionalResponse.success(
          ChangedFile.skipped(path, ChangeType.ADDED, "Untracked path is not a regular file")
      );
    }

    return GitRunner.run(repoRoot, "diff", "--no-index", DEV_NULL, path)
        .chain((listener, diffResult) -> {
          if (diffResult.exitCode() > 1) {
            throw new IllegalStateException("git diff --no-index failed for " + path);
          }
          if (DiffParser.isBinaryDiffChunk(diffResult.output())) {
            return ExceptionalResponse.success(ChangedFile.skipped(path, ChangeType.ADDED, "Binary file"));
          }
          if (!diffResult.output().isBlank()) {
            return ExceptionalResponse.success(
                toIncludedOrSkipped(path, ChangeType.ADDED, diffResult.output(), maxDiffBytes)
            );
          }

          return readUntrackedContent(filePath)
              .chain((readListener, content) -> ExceptionalResponse.success(
                  toIncludedOrSkipped(
                      path,
                      ChangeType.ADDED,
                      synthesizeAddedDiff(path, content),
                      maxDiffBytes
                  )
              ), listener);
        });
  }

  private static ExceptionalResponse<String> readUntrackedContent(final Path filePath) {
    return ExceptionalResource.of(
        () -> Files.newBufferedReader(filePath),
        reader -> reader.lines().collect(Collectors.joining(System.lineSeparator()))
    ).execute();
  }

  private static ChangedFile toChangedFile(final DiffParser.ParsedDiffEntry entry, final int maxDiffBytes) {
    return toIncludedOrSkipped(entry.path(), entry.changeType(), entry.diff(), maxDiffBytes, entry.binary());
  }

  private static ChangedFile toIncludedOrSkipped(
      final String path,
      final ChangeType changeType,
      final String diff,
      final int maxDiffBytes
  ) {
    return toIncludedOrSkipped(path, changeType, diff, maxDiffBytes, false);
  }

  private static ChangedFile toIncludedOrSkipped(
      final String path,
      final ChangeType changeType,
      final String diff,
      final int maxDiffBytes,
      final boolean binary
  ) {
    if (binary) {
      return ChangedFile.skipped(path, changeType, "Binary file");
    }
    if (diff.length() > maxDiffBytes) {
      return ChangedFile.skipped(
          path,
          changeType,
          "Diff exceeds maxDiffKb limit (" + (maxDiffBytes / 1024) + " KB)"
      );
    }
    return ChangedFile.included(path, changeType, diff);
  }

  private static String synthesizeAddedDiff(final String path, final String content) {
    return "diff --git a/" + path + " b/" + path + System.lineSeparator()
        + "new file mode 100644" + System.lineSeparator()
        + "--- " + DEV_NULL + System.lineSeparator()
        + "+++ b/" + path + System.lineSeparator()
        + content;
  }
}