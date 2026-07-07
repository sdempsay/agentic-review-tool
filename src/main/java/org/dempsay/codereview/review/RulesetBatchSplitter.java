package org.dempsay.codereview.review;

import java.util.ArrayList;
import java.util.List;
import org.dempsay.codereview.ingest.ChangedFile;

public final class RulesetBatchSplitter {

  private RulesetBatchSplitter() {
  }

  public static List<List<ChangedFile>> split(
      final List<ChangedFile> files,
      final int maxAgentDiffKb,
      final int maxFilesPerAgent
  ) {
    if (files.isEmpty()) {
      return List.of();
    }
    if (maxAgentDiffKb <= 0 && maxFilesPerAgent <= 0) {
      return List.of(List.copyOf(files));
    }

    final int maxDiffBytes = maxAgentDiffKb > 0 ? maxAgentDiffKb * 1024 : Integer.MAX_VALUE;
    final int maxFiles = maxFilesPerAgent > 0 ? maxFilesPerAgent : Integer.MAX_VALUE;

    final List<List<ChangedFile>> batches = new ArrayList<>();
    List<ChangedFile> currentBatch = new ArrayList<>();
    int currentBytes = 0;

    for (final ChangedFile file : files) {
      final int fileBytes = file.diff().length();
      final boolean mustFlush = !currentBatch.isEmpty()
          && (currentBatch.size() >= maxFiles || currentBytes + fileBytes > maxDiffBytes);

      if (mustFlush) {
        batches.add(List.copyOf(currentBatch));
        currentBatch = new ArrayList<>();
        currentBytes = 0;
      }

      currentBatch.add(file);
      currentBytes += fileBytes;
    }

    if (!currentBatch.isEmpty()) {
      batches.add(List.copyOf(currentBatch));
    }
    return List.copyOf(batches);
  }
}