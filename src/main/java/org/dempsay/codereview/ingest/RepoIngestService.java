package org.dempsay.codereview.ingest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class RepoIngestService {

  private RepoIngestService() {
  }

  public static ExceptionalResponse<List<ChangedFile>> ingest(final RepoIngestRequest request) {
    if (!GitRunner.isGitRepository(request.repoRoot())) {
      return ExceptionalSupport.fail(
          new IllegalArgumentException("Not a git repository: " + request.repoRoot().toAbsolutePath())
      );
    }

    return ExceptionalSupport.supply(() -> {
      final List<String> paths = listReviewablePaths(request);
      final List<ChangedFile> files = new ArrayList<>();
      final int maxFileBytes = request.maxFileKb() * 1024;

      for (final String relativePath : paths) {
        files.add(readRepoFile(request.repoRoot(), relativePath, maxFileBytes));
      }
      return List.copyOf(files);
    });
  }

  static List<String> listReviewablePaths(final RepoIngestRequest request) {
    final Set<String> paths = new LinkedHashSet<>();
    paths.addAll(ExceptionalSupport.response(GitRunner.runLines(request.repoRoot(), "ls-files")));
    paths.addAll(ExceptionalSupport.response(
        GitRunner.runLines(request.repoRoot(), "ls-files", "--others", "--exclude-standard")
    ));

    final List<String> reviewable = new ArrayList<>();
    for (final String path : paths) {
      if (RepoPathFilter.exclusionReason(path, request).isEmpty()) {
        reviewable.add(path);
      }
    }
    return List.copyOf(reviewable);
  }

  private static ChangedFile readRepoFile(
      final Path repoRoot,
      final String relativePath,
      final int maxFileBytes
  ) {
    final Path filePath = repoRoot.resolve(relativePath);
    if (!Files.isRegularFile(filePath)) {
      return ChangedFile.skipped(relativePath, ChangeType.EXISTING, "Not a regular file");
    }

    final String content = ExceptionalSupport.response(ExceptionalSupport.supply(() -> Files.readString(filePath)));
    if (isBinaryContent(content)) {
      return ChangedFile.skipped(relativePath, ChangeType.EXISTING, "Binary file");
    }
    if (content.length() > maxFileBytes) {
      return ChangedFile.skipped(
          relativePath,
          ChangeType.EXISTING,
          "File exceeds maxDiffKb limit (" + (maxFileBytes / 1024) + " KB)"
      );
    }
    return ChangedFile.included(relativePath, ChangeType.EXISTING, content);
  }

  private static boolean isBinaryContent(final String content) {
    return content.indexOf('\0') >= 0;
  }
}