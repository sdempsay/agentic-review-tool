package org.dempsay.codereview.review;

import java.util.List;
import java.util.Optional;
import org.dempsay.codereview.ingest.ChangedFile;

public final class FileReferenceMatcher {

  private FileReferenceMatcher() {
  }

  public static Optional<ChangedFile> findReferencedFile(
      final String message,
      final List<ChangedFile> changedFiles
  ) {
    if (message == null || message.isBlank()) {
      return Optional.empty();
    }

    final Optional<ChangedFile> byFullPath = findLongestPathMatch(message, changedFiles);
    if (byFullPath.isPresent()) {
      return byFullPath;
    }
    return findUniqueBasenameMatch(message, changedFiles);
  }

  private static Optional<ChangedFile> findLongestPathMatch(
      final String message,
      final List<ChangedFile> changedFiles
  ) {
    ChangedFile bestMatch = null;
    for (final ChangedFile file : changedFiles) {
      if (message.contains(file.path()) && (bestMatch == null || file.path().length() > bestMatch.path().length())) {
        bestMatch = file;
      }
    }
    return Optional.ofNullable(bestMatch);
  }

  private static Optional<ChangedFile> findUniqueBasenameMatch(
      final String message,
      final List<ChangedFile> changedFiles
  ) {
    ChangedFile match = null;
    int matchCount = 0;
    for (final ChangedFile file : changedFiles) {
      if (message.contains(basename(file.path()))) {
        match = file;
        matchCount++;
      }
    }
    return matchCount == 1 ? Optional.ofNullable(match) : Optional.empty();
  }

  private static String basename(final String path) {
    final int slash = path.lastIndexOf('/');
    return slash >= 0 ? path.substring(slash + 1) : path;
  }
}