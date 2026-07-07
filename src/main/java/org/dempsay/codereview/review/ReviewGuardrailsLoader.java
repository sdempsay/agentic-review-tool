package org.dempsay.codereview.review;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResource;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/**
 * Loads review-agent guardrails from {@code rulesDir/guardrails/*.md}.
 * When that directory exists, only those files are used (even if empty).
 * Otherwise falls back to bundled classpath guardrails.
 */
public final class ReviewGuardrailsLoader {

  public static final String SUBDIRECTORY = "guardrails";
  private static final List<String> BUNDLED_RESOURCES = List.of(
      "/rules/guardrails/no-modify.md",
      "/rules/guardrails/no-internet.md"
  );

  private ReviewGuardrailsLoader() {
  }

  public static ExceptionalResponse<String> load(final Path rulesDir) {
    return ExceptionalSupport.supply(() -> {
      if (rulesDir != null) {
        final Path guardrailsDir = rulesDir.resolve(SUBDIRECTORY);
        if (Files.isDirectory(guardrailsDir)) {
          return formatGuardrails(listMarkdownFiles(guardrailsDir));
        }
      }
      return formatBundledGuardrails();
    });
  }

  private static String formatGuardrails(final List<Path> files) {
    if (files.isEmpty()) {
      return "";
    }
    final List<String> sections = new ArrayList<>();
    final List<String> headings = new ArrayList<>();
    for (final Path file : files) {
      sections.add(ExceptionalSupport.response(ExceptionalSupport.supply(() -> Files.readString(file))));
      headings.add(fileNameStem(file));
    }
    return joinSections(sections, headings);
  }

  private static String joinSections(final List<String> sections, final List<String> headings) {
    final StringBuilder builder = new StringBuilder();
    builder.append("## Guardrails").append(System.lineSeparator()).append(System.lineSeparator());
    for (int index = 0; index < sections.size(); index++) {
      builder.append("### ").append(headings.get(index)).append(System.lineSeparator());
      builder.append(sections.get(index).trim()).append(System.lineSeparator()).append(System.lineSeparator());
    }
    return builder.toString().trim();
  }

  private static List<Path> listMarkdownFiles(final Path directory) {
    return ExceptionalSupport.response(ExceptionalSupport.supply(() -> {
      try (Stream<Path> paths = Files.list(directory)) {
        return paths
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".md"))
            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
            .toList();
      }
    }));
  }

  private static String fileNameStem(final Path file) {
    final String name = file.getFileName().toString();
    return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
  }

  private static String formatBundledGuardrails() {
    final List<String> sections = new ArrayList<>();
    final List<String> headings = new ArrayList<>();
    for (final String resourcePath : BUNDLED_RESOURCES) {
      sections.add(ExceptionalSupport.response(loadBundledResource(resourcePath)));
      headings.add(headingFromResource(resourcePath));
    }
    return joinSections(sections, headings);
  }

  private static ExceptionalResponse<String> loadBundledResource(final String resourcePath) {
    return ExceptionalResource.of(
        () -> openBundledStream(resourcePath),
        input -> new String(input.readAllBytes(), StandardCharsets.UTF_8)
    ).execute();
  }

  private static InputStream openBundledStream(final String resourcePath) {
    final InputStream input = ReviewGuardrailsLoader.class.getResourceAsStream(resourcePath);
    if (input == null) {
      throw new IllegalStateException("Missing bundled resource: " + resourcePath);
    }
    return input;
  }

  private static String headingFromResource(final String resourcePath) {
    final int slash = resourcePath.lastIndexOf('/');
    final String fileName = slash >= 0 ? resourcePath.substring(slash + 1) : resourcePath;
    return fileNameStem(Path.of(fileName));
  }
}