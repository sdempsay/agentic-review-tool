package org.dempsay.codereview.ingest;

import java.util.List;
import java.util.Optional;
import org.dempsay.codereview.rules.PathGlobMatcher;

public final class RepoPathFilter {

  private RepoPathFilter() {
  }

  public static Optional<String> exclusionReason(final String path, final RepoIngestRequest request) {
    if (!matchesPathGlobs(path, request.pathGlobs())) {
      return Optional.of("Outside --path scope");
    }

    final String extension = fileExtension(path);
    final List<String> includeExtensions = request.resolvedIncludeExtensions();
    if (!includeExtensions.isEmpty()) {
      if (!includeExtensions.contains(extension)) {
        return Optional.of("Extension not in --include-ext (" + extension + ")");
      }
      return Optional.empty();
    }

    if (request.resolvedExcludeExtensions().contains(extension)) {
      return Optional.of("Excluded file type (" + extension + ")");
    }
    return Optional.empty();
  }

  private static boolean matchesPathGlobs(final String path, final List<String> pathGlobs) {
    if (pathGlobs.isEmpty()) {
      return true;
    }
    for (final String glob : pathGlobs) {
      if (PathGlobMatcher.matches(glob, path)) {
        return true;
      }
    }
    return false;
  }

  static String fileExtension(final String path) {
    final String normalized = path.replace('\\', '/');
    final int slash = normalized.lastIndexOf('/');
    final int dot = normalized.lastIndexOf('.');
    if (dot < 0 || dot < slash) {
      return "";
    }
    return normalized.substring(dot).toLowerCase();
  }
}