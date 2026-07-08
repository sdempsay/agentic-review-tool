package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;

/**
 * Whether review prompts use diffs or full file contents.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public enum ReviewContentMode {
  DIFF,
  FULL_FILE;

  /**
   * Resolves the review content mode from changed files.
   * 
   * @param changedFiles the changedFiles
   * @return the result
   * @since 1.0.0
 */
  public static ReviewContentMode resolve(final List<ChangedFile> changedFiles) {
    final List<ChangedFile> reviewable = changedFiles.stream().filter(ChangedFile::hasDiff).toList();
    if (reviewable.isEmpty()) {
      return DIFF;
    }

    final boolean allRepository = reviewable.stream().allMatch(file -> file.changeType() == ChangeType.EXISTING);
    final boolean allDiff = reviewable.stream().noneMatch(file -> file.changeType() == ChangeType.EXISTING);
    if (!allRepository && !allDiff) {
      throw new IllegalArgumentException("Cannot mix repository files with diff changes in one review");
    }
    return allRepository ? FULL_FILE : DIFF;
  }
}