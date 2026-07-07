package org.dempsay.codereview.ingest;

import java.nio.file.Path;
import java.util.List;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class GitChangeLister {

  private static final int DEFAULT_MAX_DIFF_KB = 512;

  private GitChangeLister() {
  }

  public static ExceptionalResponse<List<ChangedFile>> listUncommittedChanges(final Path repoRoot) {
    return GitIngestService.ingest(IngestRequest.uncommitted(repoRoot, DEFAULT_MAX_DIFF_KB));
  }
}
