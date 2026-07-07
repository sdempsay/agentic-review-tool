package org.dempsay.codereview.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DiffParser {

  private static final Pattern DIFF_HEADER =
      Pattern.compile("^diff --git a/(.*) b/(.*)$", Pattern.MULTILINE);

  private DiffParser() {
  }

  static List<ParsedDiffEntry> parse(final String rawDiff) {
    if (rawDiff == null || rawDiff.isBlank()) {
      return List.of();
    }

    final List<ParsedDiffEntry> entries = new ArrayList<>();
    final Matcher matcher = DIFF_HEADER.matcher(rawDiff);
    final List<Integer> starts = new ArrayList<>();
    while (matcher.find()) {
      starts.add(matcher.start());
    }
    if (starts.isEmpty()) {
      return List.of();
    }

    for (int index = 0; index < starts.size(); index++) {
      final int start = starts.get(index);
      final int end = index + 1 < starts.size() ? starts.get(index + 1) : rawDiff.length();
      final String chunk = rawDiff.substring(start, end).trim();
      entries.add(parseChunk(chunk));
    }
    return entries;
  }

  private static ParsedDiffEntry parseChunk(final String chunk) {
    final Matcher matcher = DIFF_HEADER.matcher(chunk);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Diff chunk is missing git header");
    }

    final String oldPath = unquoteGitPath(matcher.group(1));
    final String newPath = unquoteGitPath(matcher.group(2));
    final String path = "/dev/null".equals(newPath) ? oldPath : newPath;
    final ChangeType changeType = detectChangeType(chunk, oldPath, newPath);
    final boolean binary = isBinaryDiffChunk(chunk);

    return new ParsedDiffEntry(path, changeType, chunk, binary);
  }

  private static ChangeType detectChangeType(final String chunk, final String oldPath, final String newPath) {
    if (chunk.contains("rename from") || chunk.contains("rename to")) {
      return ChangeType.RENAMED;
    }
    if ("/dev/null".equals(oldPath) || chunk.contains("new file mode")) {
      return ChangeType.ADDED;
    }
    if ("/dev/null".equals(newPath) || chunk.contains("deleted file mode")) {
      return ChangeType.DELETED;
    }
    return ChangeType.MODIFIED;
  }

  private static String unquoteGitPath(final String path) {
    if (path.startsWith("\"") && path.endsWith("\"")) {
      return path.substring(1, path.length() - 1).replace("\\\"", "\"");
    }
    return path;
  }

  static boolean isBinaryDiffChunk(final String chunk) {
    return chunk.lines()
        .anyMatch(line -> line.startsWith("Binary files ") && line.endsWith(" differ"));
  }

  record ParsedDiffEntry(String path, ChangeType changeType, String diff, boolean binary) {
  }
}