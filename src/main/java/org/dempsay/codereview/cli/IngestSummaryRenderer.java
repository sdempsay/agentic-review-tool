package org.dempsay.codereview.cli;

import java.util.List;
import org.dempsay.codereview.ingest.ChangedFile;

public final class IngestSummaryRenderer {

  private IngestSummaryRenderer() {
  }

  public static void render(final List<ChangedFile> changedFiles) {
    final long included = changedFiles.stream().filter(ChangedFile::included).count();
    final long skipped = changedFiles.size() - included;
    final long withDiff = changedFiles.stream().filter(ChangedFile::hasDiff).count();

    System.out.printf(
        "Ingested %d file(s): %d with diff, %d skipped%n",
        changedFiles.size(),
        withDiff,
        skipped
    );

    for (final ChangedFile file : changedFiles) {
      if (file.included()) {
        System.out.printf(
            "  %s [%s, %d bytes]%n",
            file.path(),
            file.changeType(),
            file.diff().length()
        );
      } else {
        System.out.printf("  %s [%s, skipped: %s]%n", file.path(), file.changeType(), file.skipReason());
      }
    }
  }
}