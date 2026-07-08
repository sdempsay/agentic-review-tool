package org.dempsay.codereview.ingest;

/**
 * A repository file included or skipped during ingest.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record ChangedFile(
    String path,
    ChangeType changeType,
    String diff,
    boolean included,
    String skipReason
) {

  /**
   * Creates a new ChangedFile.
   * 
   * @since 1.0.0
 */
  public ChangedFile {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("path is required");
    }
  }

  /**
   * Creates an included changed file with diff content.
   * 
   * @param path the path
   * @param changeType the changeType
   * @param diff the diff
   * @return the result
   * @since 1.0.0
 */
  public static ChangedFile included(
      final String path,
      final ChangeType changeType,
      final String diff
  ) {
    return new ChangedFile(path, changeType, diff == null ? "" : diff, true, null);
  }

  /**
   * Creates a skipped changed file with a reason.
   * 
   * @param path the path
   * @param changeType the changeType
   * @param skipReason the skipReason
   * @return the result
   * @since 1.0.0
 */
  public static ChangedFile skipped(
      final String path,
      final ChangeType changeType,
      final String skipReason
  ) {
    return new ChangedFile(path, changeType, "", false, skipReason);
  }

  /**
   * Returns whether this file has reviewable diff content.
   * 
   * @return the result
   * @since 1.0.0
 */
  public boolean hasDiff() {
    return included && diff != null && !diff.isBlank();
  }
}