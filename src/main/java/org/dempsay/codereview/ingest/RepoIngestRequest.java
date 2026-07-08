package org.dempsay.codereview.ingest;

import java.nio.file.Path;
import java.util.List;

public record RepoIngestRequest(
    Path repoRoot,
    int maxFileKb,
    List<String> pathGlobs,
    List<String> includeExtensions,
    List<String> configExcludeExtensions,
    List<String> excludeExtensions
) {

  public RepoIngestRequest {
    if (repoRoot == null) {
      throw new IllegalArgumentException("repoRoot is required");
    }
    if (maxFileKb <= 0) {
      throw new IllegalArgumentException("maxFileKb must be positive");
    }
    pathGlobs = List.copyOf(pathGlobs == null ? List.of() : pathGlobs);
    includeExtensions = List.copyOf(includeExtensions == null ? List.of() : includeExtensions);
    configExcludeExtensions = List.copyOf(configExcludeExtensions == null ? List.of() : configExcludeExtensions);
    excludeExtensions = List.copyOf(excludeExtensions == null ? List.of() : excludeExtensions);
  }

  public static RepoIngestRequest of(final Path repoRoot, final int maxFileKb) {
    return new RepoIngestRequest(repoRoot, maxFileKb, List.of(), List.of(), List.of(), List.of());
  }

  public List<String> resolvedExcludeExtensions() {
    if (!includeExtensions.isEmpty()) {
      return IngestExtensionFilter.normalizeExtensions(excludeExtensions);
    }
    return IngestExtensionFilter.resolvedExcludeExtensions(configExcludeExtensions, excludeExtensions);
  }

  public List<String> resolvedIncludeExtensions() {
    return IngestExtensionFilter.normalizeExtensions(includeExtensions);
  }
}