package org.dempsay.codereview.ingest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class RepoIngestService {

  private RepoIngestService() {
  }

  public static ExceptionalResponse<List<ChangedFile>> ingest(final RepoIngestRequest request) {
    return ingest(request, null);
  }

  public static ExceptionalResponse<List<ChangedFile>> ingest(
      final RepoIngestRequest request,
      final ExceptionalListener listener
  ) {
    if (!GitRunner.isGitRepository(request.repoRoot())) {
      return ExceptionalSupport.fail(
          listener,
          new IllegalArgumentException("Not a git repository: " + request.repoRoot().toAbsolutePath())
      );
    }

    return listReviewablePaths(request, listener)
        .chain((pathsListener, paths) -> readAllRepoFiles(request, paths, 0, new ArrayList<>(), pathsListener), listener);
  }

  private static ExceptionalResponse<List<String>> listReviewablePaths(
      final RepoIngestRequest request,
      final ExceptionalListener listener
  ) {
    return GitRunner.runLines(request.repoRoot(), listener, "ls-files")
        .chain((trackedListener, tracked) -> GitRunner.runLines(
            request.repoRoot(),
            trackedListener,
            "ls-files",
            "--others",
            "--exclude-standard"
        ).chain((untrackedListener, untracked) -> {
          final Set<String> paths = new LinkedHashSet<>();
          paths.addAll(tracked);
          paths.addAll(untracked);

          final List<String> reviewable = new ArrayList<>();
          for (final String path : paths) {
            if (RepoPathFilter.exclusionReason(path, request).isEmpty()) {
              reviewable.add(path);
            }
          }
          return ExceptionalResponse.success(List.copyOf(reviewable));
        }, trackedListener), listener);
  }

  private static ExceptionalResponse<List<ChangedFile>> readAllRepoFiles(
      final RepoIngestRequest request,
      final List<String> paths,
      final int index,
      final List<ChangedFile> files,
      final ExceptionalListener listener
  ) {
    if (index >= paths.size()) {
      return ExceptionalResponse.success(List.copyOf(files));
    }

    return readRepoFile(request.repoRoot(), paths.get(index), request.maxFileKb() * 1024, listener)
        .chain((fileListener, changedFile) ->
            readAllRepoFiles(request, paths, index + 1, append(files, changedFile), listener),
            listener
        );
  }

  private static List<ChangedFile> append(final List<ChangedFile> files, final ChangedFile changedFile) {
    final List<ChangedFile> next = new ArrayList<>(files);
    next.add(changedFile);
    return next;
  }

  private static ExceptionalResponse<ChangedFile> readRepoFile(
      final Path repoRoot,
      final String relativePath,
      final int maxFileBytes,
      final ExceptionalListener listener
  ) {
    final Path filePath = repoRoot.resolve(relativePath);
    if (!Files.isRegularFile(filePath)) {
      return ExceptionalResponse.success(
          ChangedFile.skipped(relativePath, ChangeType.EXISTING, "Not a regular file")
      );
    }

    return ExceptionalSupport.supply(() -> Files.readString(filePath), listener)
        .chain((readListener, content) -> {
          if (isBinaryContent(content)) {
            return ExceptionalResponse.success(
                ChangedFile.skipped(relativePath, ChangeType.EXISTING, "Binary file")
            );
          }
          if (content.length() > maxFileBytes) {
            return ExceptionalResponse.success(
                ChangedFile.skipped(
                    relativePath,
                    ChangeType.EXISTING,
                    "File exceeds maxDiffKb limit (" + (maxFileBytes / 1024) + " KB)"
                )
            );
          }
          return ExceptionalResponse.success(ChangedFile.included(relativePath, ChangeType.EXISTING, content));
        }, listener);
  }

  private static boolean isBinaryContent(final String content) {
    return content.indexOf('\0') >= 0;
  }
}