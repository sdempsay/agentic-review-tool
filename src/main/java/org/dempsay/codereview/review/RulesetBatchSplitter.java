package org.dempsay.codereview.review;

import java.util.ArrayList;
import java.util.List;
import org.dempsay.codereview.ingest.ChangedFile;

/**
 * Splits files into batches that fit agent diff caps.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class RulesetBatchSplitter {

  private RulesetBatchSplitter() {
  }

  /**
   * A batch of files and whether it exceeds the context cap.
   *
   * @param files files in this batch
   * @param exceedsContextCap whether combined diff exceeds the hard context cap
   * @since 1.0.0
   * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
   */
  public record BatchChunk(List<ChangedFile> files, boolean exceedsContextCap) {

    /**
     * Defensive copy of batch files.
     *
     * @since 1.0.0
     */
    public BatchChunk {
      files = List.copyOf(files);
    }
  }

  /**
   * Splits files into batches within agent diff caps.
   * 
   * @param files the files
   * @param limits the limits
   * @return the result
   * @since 1.0.0
 */
  public static List<BatchChunk> split(
      final List<ChangedFile> files,
      final AgentBatchLimits limits
  ) {
    if (files.isEmpty()) {
      return List.of();
    }
    if (!limits.hasSoftCap() && !limits.hasHardCap() && limits.maxFilesPerAgent() <= 0) {
      return List.of(singleChunk(files, limits));
    }

    final int softTarget = limits.hasSoftCap() ? limits.softDiffBytes() : Integer.MAX_VALUE;
    final int hardLimit = limits.hasHardCap() ? limits.hardDiffBytes() : Integer.MAX_VALUE;
    final int maxFiles = limits.maxFilesPerAgent() > 0 ? limits.maxFilesPerAgent() : Integer.MAX_VALUE;

    final List<BatchChunk> batches = new ArrayList<>();
    List<ChangedFile> currentBatch = new ArrayList<>();
    int currentBytes = 0;

    for (final ChangedFile file : files) {
      final int fileBytes = file.diff().length();
      if (!currentBatch.isEmpty()) {
        final boolean exceedsFiles = currentBatch.size() >= maxFiles;
        final boolean exceedsHard = currentBytes + fileBytes > hardLimit;
        final boolean exceedsSoft = currentBytes + fileBytes > softTarget;

        if (exceedsHard || exceedsFiles || exceedsSoft) {
          batches.add(toChunk(currentBatch, currentBytes, limits));
          currentBatch = new ArrayList<>();
          currentBytes = 0;
        }
      }

      currentBatch.add(file);
      currentBytes += fileBytes;
    }

    if (!currentBatch.isEmpty()) {
      batches.add(toChunk(currentBatch, currentBytes, limits));
    }
    return List.copyOf(batches);
  }

  /**
   * Splits files into batches within agent diff caps.
   * 
   * @param files the files
   * @param maxAgentDiffKb the maxAgentDiffKb
   * @param maxFilesPerAgent the maxFilesPerAgent
   * @return the result
   * @since 1.0.0
 */
  public static List<List<ChangedFile>> split(
      final List<ChangedFile> files,
      final int maxAgentDiffKb,
      final int maxFilesPerAgent
  ) {
    final AgentBatchLimits limits = AgentBatchLimits.fromLegacyCaps(maxAgentDiffKb, maxFilesPerAgent, 0);
    return split(files, limits).stream().map(BatchChunk::files).toList();
  }

  private static BatchChunk singleChunk(final List<ChangedFile> files, final AgentBatchLimits limits) {
    final int diffBytes = files.stream().mapToInt(file -> file.diff().length()).sum();
    return new BatchChunk(files, limits.exceedsHardCap(diffBytes));
  }

  private static BatchChunk toChunk(
      final List<ChangedFile> files,
      final int diffBytes,
      final AgentBatchLimits limits
  ) {
    return new BatchChunk(List.copyOf(files), limits.exceedsHardCap(diffBytes));
  }
}