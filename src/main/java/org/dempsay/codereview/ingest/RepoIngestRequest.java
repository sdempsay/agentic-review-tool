package org.dempsay.codereview.ingest;

import java.nio.file.Path;
import java.util.List;

public record RepoIngestRequest(
    Path repoRoot,
    int maxFileKb,
    List<String> pathGlobs,
    List<String> includeExtensions,
    List<String> excludeExtensions
) {

  private static final List<String> DEFAULT_EXCLUDE_EXTENSIONS = List.of(".md", ".json");

  public RepoIngestRequest {
    if (repoRoot == null) {
      throw new IllegalArgumentException("repoRoot is required");
    }
    if (maxFileKb <= 0) {
      throw new IllegalArgumentException("maxFileKb must be positive");
    }
    pathGlobs = List.copyOf(pathGlobs == null ? List.of() : pathGlobs);
    includeExtensions = List.copyOf(includeExtensions == null ? List.of() : includeExtensions);
    excludeExtensions = List.copyOf(excludeExtensions == null ? List.of() : excludeExtensions);
  }

  public static RepoIngestRequest of(final Path repoRoot, final int maxFileKb) {
    return new RepoIngestRequest(repoRoot, maxFileKb, List.of(), List.of(), List.of());
  }

  public List<String> resolvedExcludeExtensions() {
    if (!includeExtensions.isEmpty()) {
      return normalizeExtensions(excludeExtensions);
    }
    final java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>(DEFAULT_EXCLUDE_EXTENSIONS);
    merged.addAll(normalizeExtensions(excludeExtensions));
    return List.copyOf(merged);
  }

  public List<String> resolvedIncludeExtensions() {
    return normalizeExtensions(includeExtensions);
  }

  private static List<String> normalizeExtensions(final List<String> extensions) {
    return extensions.stream()
        .map(RepoIngestRequest::normalizeExtension)
        .filter(extension -> !extension.isBlank())
        .distinct()
        .toList();
  }

  private static String normalizeExtension(final String extension) {
    if (extension == null || extension.isBlank()) {
      return "";
    }
    final String trimmed = extension.trim().toLowerCase();
    return trimmed.startsWith(".") ? trimmed : "." + trimmed;
  }
}