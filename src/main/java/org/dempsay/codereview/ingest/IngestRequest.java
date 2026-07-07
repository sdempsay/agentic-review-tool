package org.dempsay.codereview.ingest;

import java.nio.file.Path;

public record IngestRequest(Path repoRoot, DiffScope scope, String baseRef, int maxDiffKb) {

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

  public static IngestRequest uncommitted(final Path repoRoot, final int maxDiffKb) {
    return new IngestRequest(repoRoot, DiffScope.UNCOMMITTED, null, maxDiffKb);
  }

  public static IngestRequest staged(final Path repoRoot, final int maxDiffKb) {
    return new IngestRequest(repoRoot, DiffScope.STAGED, null, maxDiffKb);
  }

  public static IngestRequest againstBase(final Path repoRoot, final String baseRef, final int maxDiffKb) {
    return new IngestRequest(repoRoot, DiffScope.BASE, baseRef, maxDiffKb);
  }
}