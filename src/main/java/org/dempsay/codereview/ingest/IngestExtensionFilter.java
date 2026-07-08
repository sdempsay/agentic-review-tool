package org.dempsay.codereview.ingest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Shared default extension exclusions for diff and repo ingest. */
public final class IngestExtensionFilter {

  public static final List<String> DEFAULT_EXCLUDE_EXTENSIONS = List.of(".md", ".json");

  private IngestExtensionFilter() {
  }

  public static Optional<String> defaultExclusionReason(final String path) {
    return exclusionReason(path, List.of(), List.of(), List.of());
  }

  public static Optional<String> exclusionReason(
      final String path,
      final List<String> includeExtensions,
      final List<String> configExcludeExtensions,
      final List<String> cliExcludeExtensions
  ) {
    final String extension = RepoPathFilter.fileExtension(path);
    final List<String> includes = normalizeExtensions(includeExtensions);
    if (!includes.isEmpty()) {
      if (!includes.contains(extension)) {
        return Optional.of("Extension not in --include-ext (" + extension + ")");
      }
      return Optional.empty();
    }

    if (resolvedExcludeExtensions(configExcludeExtensions, cliExcludeExtensions).contains(extension)) {
      return Optional.of("Excluded file type (" + extension + ")");
    }
    return Optional.empty();
  }

  public static List<String> resolvedExcludeExtensions(
      final List<String> configExcludeExtensions,
      final List<String> cliExcludeExtensions
  ) {
    final LinkedHashSet<String> merged = new LinkedHashSet<>(DEFAULT_EXCLUDE_EXTENSIONS);
    merged.addAll(normalizeExtensions(configExcludeExtensions));
    merged.addAll(normalizeExtensions(cliExcludeExtensions));
    return List.copyOf(merged);
  }

  static List<String> normalizeExtensions(final List<String> extensions) {
    return extensions.stream()
        .map(IngestExtensionFilter::normalizeExtension)
        .filter(extension -> !extension.isBlank())
        .distinct()
        .toList();
  }

  private static String normalizeExtension(final String extension) {
    if (extension == null || extension.isBlank()) {
      return "";
    }
    final String trimmed = extension.trim().toLowerCase();
    return trimmed.startsWith(".") ? trimmed : "." + trimmed;
  }
}