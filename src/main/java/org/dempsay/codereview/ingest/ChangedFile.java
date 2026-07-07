package org.dempsay.codereview.ingest;

public record ChangedFile(
    String path,
    ChangeType changeType,
    String diff,
    boolean included,
    String skipReason
) {

  public ChangedFile {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("path is required");
    }
  }

  public static ChangedFile included(
      final String path,
      final ChangeType changeType,
      final String diff
  ) {
    return new ChangedFile(path, changeType, diff == null ? "" : diff, true, null);
  }

  public static ChangedFile skipped(
      final String path,
      final ChangeType changeType,
      final String skipReason
  ) {
    return new ChangedFile(path, changeType, "", false, skipReason);
  }

  public boolean hasDiff() {
    return included && diff != null && !diff.isBlank();
  }
}