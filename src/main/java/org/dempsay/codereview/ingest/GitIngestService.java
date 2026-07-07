package org.dempsay.codereview.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class GitIngestService {

  private static final String DEV_NULL = "/dev/null";

  private GitIngestService() {
  }

  public static ExceptionalResponse<List<ChangedFile>> ingest(final IngestRequest request) {
    return ExceptionalSupport.supply(() -> ingestRequired(request));
  }

  public static List<ChangedFile> ingestRequired(final IngestRequest request)
      throws IOException, InterruptedException {
    if (!GitRunner.isGitRepository(request.repoRoot())) {
      throw new IllegalArgumentException("Not a git repository: " + request.repoRoot().toAbsolutePath());
    }

    return switch (request.scope()) {
      case UNCOMMITTED -> ingestUncommitted(request);
      case STAGED -> ingestFromDiff(request, "diff", "--cached");
      case BASE -> ingestFromDiff(request, "diff", request.baseRef() + "...HEAD");
    };
  }

  private static List<ChangedFile> ingestUncommitted(final IngestRequest request)
      throws IOException, InterruptedException {
    final Map<String, ChangedFile> files = new LinkedHashMap<>();
    final int maxDiffBytes = request.maxDiffKb() * 1024;

    if (GitRunner.hasCommits(request.repoRoot())) {
      addParsedDiff(files, GitRunner.run(request.repoRoot(), "diff", "HEAD").output(), maxDiffBytes);
    }

    final Set<String> untracked = new LinkedHashSet<>(
        GitRunner.runLines(request.repoRoot(), "ls-files", "--others", "--exclude-standard")
    );
    for (final String path : untracked) {
      if (files.containsKey(path)) {
        continue;
      }
      files.put(path, ingestUntrackedFile(request.repoRoot(), path, maxDiffBytes));
    }

    return List.copyOf(files.values());
  }

  private static List<ChangedFile> ingestFromDiff(
      final IngestRequest request,
      final String... gitDiffCommand
  ) throws IOException, InterruptedException {
    final GitRunner.GitResult result = GitRunner.run(request.repoRoot(), gitDiffCommand);
    if (result.exitCode() != 0) {
      throw new IllegalStateException(
          "git " + String.join(" ", gitDiffCommand) + " failed with exit code " + result.exitCode()
      );
    }

    final Map<String, ChangedFile> files = new LinkedHashMap<>();
    addParsedDiff(files, result.output(), request.maxDiffKb() * 1024);
    return List.copyOf(files.values());
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

  private static ChangedFile ingestUntrackedFile(
      final Path repoRoot,
      final String path,
      final int maxDiffBytes
  ) throws IOException, InterruptedException {
    final Path filePath = repoRoot.resolve(path);
    if (!Files.isRegularFile(filePath)) {
      return ChangedFile.skipped(path, ChangeType.ADDED, "Untracked path is not a regular file");
    }

    final GitRunner.GitResult diffResult = GitRunner.run(
        repoRoot,
        "diff",
        "--no-index",
        DEV_NULL,
        path
    );
    if (diffResult.exitCode() > 1) {
      throw new IllegalStateException("git diff --no-index failed for " + path);
    }
    if (DiffParser.isBinaryDiffChunk(diffResult.output())) {
      return ChangedFile.skipped(path, ChangeType.ADDED, "Binary file");
    }

    final String diff = diffResult.output().isBlank()
        ? synthesizeAddedDiff(path, Files.readString(filePath))
        : diffResult.output();

    if (diff.length() > maxDiffBytes) {
      return ChangedFile.skipped(
          path,
          ChangeType.ADDED,
          "Diff exceeds maxDiffKb limit (" + (maxDiffBytes / 1024) + " KB)"
      );
    }
    return ChangedFile.included(path, ChangeType.ADDED, diff);
  }

  private static ChangedFile toChangedFile(final DiffParser.ParsedDiffEntry entry, final int maxDiffBytes) {
    if (entry.binary()) {
      return ChangedFile.skipped(entry.path(), entry.changeType(), "Binary file");
    }
    if (entry.diff().length() > maxDiffBytes) {
      return ChangedFile.skipped(
          entry.path(),
          entry.changeType(),
          "Diff exceeds maxDiffKb limit (" + (maxDiffBytes / 1024) + " KB)"
      );
    }
    return ChangedFile.included(entry.path(), entry.changeType(), entry.diff());
  }

  private static String synthesizeAddedDiff(final String path, final String content) {
    return "diff --git a/" + path + " b/" + path + System.lineSeparator()
        + "new file mode 100644" + System.lineSeparator()
        + "--- " + DEV_NULL + System.lineSeparator()
        + "+++ b/" + path + System.lineSeparator()
        + content;
  }
}