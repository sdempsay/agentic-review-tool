package org.dempsay.codereview.ingest;

import java.nio.file.Path;

/**
 * Parameters for git-based diff ingest.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record IngestRequest(Path repoRoot, DiffScope scope, String baseRef, int maxDiffKb) {

  /**
   * Creates a new IngestRequest.
   * 
   * @since 1.0.0
 */
  public IngestRequest {
    if (repoRoot == null) {
      throw new IllegalArgumentException("repoRoot is required");
    }
    if (scope == DiffScope.BASE && (baseRef == null || baseRef.isBlank())) {
      throw new IllegalArgumentException("baseRef is required when scope is BASE");
    }
    if (maxDiffKb <= 0) {
      throw new IllegalArgumentException("maxDiffKb must be positive");
    }
  }

  /**
   * Creates an ingest request for uncommitted changes.
   * 
   * @param repoRoot the repoRoot
   * @param maxDiffKb the maxDiffKb
   * @return the result
   * @since 1.0.0
 */
  public static IngestRequest uncommitted(final Path repoRoot, final int maxDiffKb) {
    return new IngestRequest(repoRoot, DiffScope.UNCOMMITTED, null, maxDiffKb);
  }

  /**
   * Creates an ingest request for staged changes.
   * 
   * @param repoRoot the repoRoot
   * @param maxDiffKb the maxDiffKb
   * @return the result
   * @since 1.0.0
 */
  public static IngestRequest staged(final Path repoRoot, final int maxDiffKb) {
    return new IngestRequest(repoRoot, DiffScope.STAGED, null, maxDiffKb);
  }

  /**
   * Creates an ingest request for changes against a base ref.
   * 
   * @param repoRoot the repoRoot
   * @param baseRef the baseRef
   * @param maxDiffKb the maxDiffKb
   * @return the result
   * @since 1.0.0
 */
  public static IngestRequest againstBase(final Path repoRoot, final String baseRef, final int maxDiffKb) {
    return new IngestRequest(repoRoot, DiffScope.BASE, baseRef, maxDiffKb);
  }
}