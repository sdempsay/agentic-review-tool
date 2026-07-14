package org.dempsay.codereview.review;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dempsay.codereview.ingest.ChangedFile;

/**
 * Indexes new-file line numbers introduced on {@code +} diff lines per path.
 *
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
final class DiffLineIndex {

  private static final Pattern HUNK_HEADER =
      Pattern.compile("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@");
  private static final Pattern WILDCARD_IMPORT =
      Pattern.compile("import\\s+[\\w.]+\\.\\*\\s*;");

  private final Map<String, Set<Integer>> addedLinesByPath;
  private final Map<String, String> diffByPath;

  DiffLineIndex(final Map<String, Set<Integer>> addedLinesByPath, final Map<String, String> diffByPath) {
    this.addedLinesByPath = addedLinesByPath;
    this.diffByPath = diffByPath;
  }

  /**
   * Builds an index from scoped changed files.
   *
   * @param files the files
   * @return the index
   * @since 1.0.0
   */
  static DiffLineIndex fromFiles(final List<ChangedFile> files) {
    final Map<String, Set<Integer>> addedLines = new HashMap<>();
    final Map<String, String> diffs = new HashMap<>();
    for (final ChangedFile file : files) {
      if (!file.hasDiff()) {
        continue;
      }
      addedLines.put(file.path(), indexAddedLines(file.diff()));
      diffs.put(file.path(), file.diff());
    }
    return new DiffLineIndex(addedLines, diffs);
  }

  /**
   * Returns whether the path has a {@code +} line at the given line number.
   *
   * @param path the path
   * @param lineNumber the lineNumber
   * @return the result
   * @since 1.0.0
   */
  boolean hasAddedLine(final String path, final int lineNumber) {
    final Set<Integer> lines = addedLinesByPath.get(path);
    return lines != null && lines.contains(lineNumber);
  }

  /**
   * Returns whether the scoped diff contains a wildcard import.
   *
   * @param path the path
   * @return the result
   * @since 1.0.0
   */
  boolean hasWildcardImport(final String path) {
    final String diff = diffByPath.get(path);
    return diff != null && WILDCARD_IMPORT.matcher(diff).find();
  }

  /**
   * Returns the new-file line text for a {@code +} or context line, if present.
   *
   * @param path the path
   * @param lineNumber the lineNumber
   * @return the line text without diff prefix, or empty when unknown
   * @since 1.0.0
   */
  String lineText(final String path, final int lineNumber) {
    final String diff = diffByPath.get(path);
    if (diff == null || diff.isBlank()) {
      return "";
    }

    int newLine = 0;
    boolean inHunk = false;
    for (final String rawLine : diff.split("\\R", -1)) {
      final Matcher header = HUNK_HEADER.matcher(rawLine);
      if (header.find()) {
        newLine = Integer.parseInt(header.group(1));
        inHunk = true;
        continue;
      }
      if (!inHunk) {
        continue;
      }
      if (rawLine.startsWith("+") && !rawLine.startsWith("+++")) {
        if (newLine == lineNumber) {
          return rawLine.substring(1);
        }
        newLine++;
        continue;
      }
      if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) {
        if (newLine == lineNumber) {
          return rawLine.substring(1);
        }
        newLine++;
        continue;
      }
      if (rawLine.startsWith("-") && !rawLine.startsWith("---")) {
        continue;
      }
      if (rawLine.startsWith("@@")) {
        final Matcher nested = HUNK_HEADER.matcher(rawLine);
        if (nested.find()) {
          newLine = Integer.parseInt(nested.group(1));
        }
      }
    }
    return "";
  }

  private static Set<Integer> indexAddedLines(final String diff) {
    final Set<Integer> added = new HashSet<>();
    int newLine = 0;
    boolean inHunk = false;

    for (final String rawLine : diff.split("\\R", -1)) {
      final Matcher header = HUNK_HEADER.matcher(rawLine);
      if (header.find()) {
        newLine = Integer.parseInt(header.group(1));
        inHunk = true;
        continue;
      }
      if (!inHunk) {
        continue;
      }
      if (rawLine.startsWith("+") && !rawLine.startsWith("+++")) {
        added.add(newLine);
        newLine++;
        continue;
      }
      if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) {
        newLine++;
        continue;
      }
      if (rawLine.startsWith("-") && !rawLine.startsWith("---")) {
        continue;
      }
      if (rawLine.startsWith("@@")) {
        final Matcher nested = HUNK_HEADER.matcher(rawLine);
        if (nested.find()) {
          newLine = Integer.parseInt(nested.group(1));
        }
      }
    }
    return added;
  }
}
