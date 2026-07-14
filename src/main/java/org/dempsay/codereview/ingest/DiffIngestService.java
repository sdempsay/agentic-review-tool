package org.dempsay.codereview.ingest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/**
 * Ingests changed files from unified diff text (files or stdin), without git.
 *
 * @since 1.1.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class DiffIngestService {

  private DiffIngestService() {
  }

  /**
   * Parses unified diff text into changed files.
   *
   * @param rawDiff the rawDiff
   * @param maxDiffKb the maxDiffKb
   * @return the result
   * @since 1.1.0
   */
  public static ExceptionalResponse<List<ChangedFile>> ingestText(
      final String rawDiff,
      final int maxDiffKb
  ) {
    return ingestText(rawDiff, maxDiffKb, null);
  }

  /**
   * Parses unified diff text into changed files.
   *
   * @param rawDiff the rawDiff
   * @param maxDiffKb the maxDiffKb
   * @param listener the listener
   * @return the result
   * @since 1.1.0
   */
  public static ExceptionalResponse<List<ChangedFile>> ingestText(
      final String rawDiff,
      final int maxDiffKb,
      final ExceptionalListener listener
  ) {
    return ExceptionalSupport.supply(() -> parse(rawDiff, maxDiffKb), listener);
  }

  /**
   * Reads unified diff content from stdin and/or files and parses it into changed files.
   *
   * @param readStdin when true, read diff text from stdin before file contents
   * @param diffFiles optional diff file paths (may be empty when {@code readStdin} is true)
   * @param maxDiffKb the maxDiffKb
   * @param listener the listener
   * @return the result
   * @since 1.1.0
   */
  public static ExceptionalResponse<List<ChangedFile>> ingestExternal(
      final boolean readStdin,
      final List<Path> diffFiles,
      final int maxDiffKb,
      final ExceptionalListener listener
  ) {
    if (!readStdin && (diffFiles == null || diffFiles.isEmpty())) {
      return ExceptionalSupport.fail(
          listener,
          new IllegalArgumentException("Provide --stdin and/or at least one --diff-file")
      );
    }
    return readExternalDiff(readStdin, diffFiles == null ? List.of() : diffFiles, listener)
        .chain((readListener, combined) -> ingestText(combined, maxDiffKb, readListener), listener);
  }

  static List<ChangedFile> parse(final String rawDiff, final int maxDiffKb) {
    final Map<String, ChangedFile> files = new LinkedHashMap<>();
    addParsedDiff(files, rawDiff, maxDiffKb * 1024);
    return List.copyOf(files.values());
  }

  static void addParsedDiff(
      final Map<String, ChangedFile> files,
      final String rawDiff,
      final int maxDiffBytes
  ) {
    for (final DiffParser.ParsedDiffEntry entry : DiffParser.parse(rawDiff)) {
      if (IngestExtensionFilter.defaultExclusionReason(entry.path()).isPresent()) {
        continue;
      }
      files.put(entry.path(), toChangedFile(entry, maxDiffBytes));
    }
  }

  private static ExceptionalResponse<String> readExternalDiff(
      final boolean readStdin,
      final List<Path> diffFiles,
      final ExceptionalListener listener
  ) {
    return ExceptionalSupport.supply(() -> {
      final StringBuilder combined = new StringBuilder();
      if (readStdin) {
        combined.append(new String(System.in.readAllBytes(), StandardCharsets.UTF_8));
      }
      for (final Path diffFile : diffFiles) {
        appendSeparator(combined);
        combined.append(Files.readString(diffFile));
      }
      return combined.toString();
    }, listener);
  }

  private static void appendSeparator(final StringBuilder combined) {
    if (!combined.isEmpty() && combined.charAt(combined.length() - 1) != '\n') {
      combined.append(System.lineSeparator());
    }
  }

  private static ChangedFile toChangedFile(final DiffParser.ParsedDiffEntry entry, final int maxDiffBytes) {
    if (entry.binary()) {
      return ChangedFile.skipped(entry.path(), entry.changeType(), "Binary file");
    }
    if (entry.diff().length() > maxDiffBytes) {
      return ChangedFile.skipped(
          entry.path(),
          entry.changeType(),
          "Diff exceeds maxDiffKb limit (" + (maxDiffBytes / 1024) + " KB)"
      );
    }
    return ChangedFile.included(entry.path(), entry.changeType(), entry.diff());
  }
}