package org.dempsay.codereview.ingest;

import java.nio.file.Path;
import java.util.List;

/**
 * Parameters for full-repository file ingest.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record RepoIngestRequest(
    Path repoRoot,
    int maxFileKb,
    List<String> pathGlobs,
    List<String> includeExtensions,
    List<String> configExcludeExtensions,
    List<String> excludeExtensions
) {

  /**
   * Creates a new RepoIngestRequest.
   * 
   * @since 1.0.0
 */
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

  /**
   * Creates a request with default optional fields.
   * 
   * @param repoRoot the repoRoot
   * @param maxFileKb the maxFileKb
   * @return the result
   * @since 1.0.0
 */
  public static RepoIngestRequest of(final Path repoRoot, final int maxFileKb) {
    return new RepoIngestRequest(repoRoot, maxFileKb, List.of(), List.of(), List.of(), List.of());
  }

  /**
   * Returns merged exclude extensions for ingest.
   * 
   * @return the result
   * @since 1.0.0
 */
  public List<String> resolvedExcludeExtensions() {
    if (!includeExtensions.isEmpty()) {
      return IngestExtensionFilter.normalizeExtensions(excludeExtensions);
    }
    return IngestExtensionFilter.resolvedExcludeExtensions(configExcludeExtensions, excludeExtensions);
  }

  /**
   * Returns normalized include extensions.
   * 
   * @return the result
   * @since 1.0.0
 */
  public List<String> resolvedIncludeExtensions() {
    return IngestExtensionFilter.normalizeExtensions(includeExtensions);
  }
}