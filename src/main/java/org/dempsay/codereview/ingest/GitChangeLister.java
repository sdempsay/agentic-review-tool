package org.dempsay.codereview.ingest;

import java.nio.file.Path;
import java.util.List;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/**
 * Lists uncommitted changes for quick inspection.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class GitChangeLister {

  private static final int DEFAULT_MAX_DIFF_KB = 512;

  private GitChangeLister() {
  }

  /**
   * Lists uncommitted changed files in a repository.
   * 
   * @param repoRoot the repoRoot
   * @return the result
   * @since 1.0.0
 */
  public static ExceptionalResponse<List<ChangedFile>> listUncommittedChanges(final Path repoRoot) {
    return GitIngestService.ingest(IngestRequest.uncommitted(repoRoot, DEFAULT_MAX_DIFF_KB));
  }
}
