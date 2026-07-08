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
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResource;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/**
 * Loads review-agent guardrails from {@code rulesDir/guardrails/*.md}.
 * When that directory exists, only those files are used (even if empty).
 * Otherwise falls back to bundled classpath guardrails.
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class ReviewGuardrailsLoader {

  public static final String SUBDIRECTORY = "guardrails";
  private static final List<String> BUNDLED_RESOURCES = List.of(
      "/rules/guardrails/no-modify.md",
      "/rules/guardrails/no-internet.md"
  );

  private ReviewGuardrailsLoader() {
  }

  /**
   * Loads configuration or resources.
   * 
   * @param rulesDir the rulesDir
   * @return the result
   * @since 1.0.0
 */
  public static ExceptionalResponse<String> load(final Path rulesDir) {
    return load(rulesDir, null);
  }

  /**
   * Loads configuration or resources.
   * 
   * @param rulesDir the rulesDir
   * @param listener the listener
   * @return the result
   * @since 1.0.0
 */
  public static ExceptionalResponse<String> load(final Path rulesDir, final ExceptionalListener listener) {
    if (rulesDir != null) {
      final Path guardrailsDir = rulesDir.resolve(SUBDIRECTORY);
      if (Files.isDirectory(guardrailsDir)) {
        return listMarkdownFiles(guardrailsDir, listener)
            .chain((listListener, files) -> formatGuardrails(files, listListener), listener);
      }
    }
    return formatBundledGuardrails(listener);
  }

  private static ExceptionalResponse<String> formatGuardrails(
      final List<Path> files,
      final ExceptionalListener listener
  ) {
    if (files.isEmpty()) {
      return ExceptionalResponse.success("");
    }
    return readGuardrailFiles(files, 0, new ArrayList<>(), new ArrayList<>(), listener)
        .chain((readListener, sections) -> ExceptionalResponse.success(
            joinSections(sections.sections(), sections.headings())
        ), listener);
  }

  private record GuardrailSections(List<String> sections, List<String> headings) {
  }

  private static ExceptionalResponse<GuardrailSections> readGuardrailFiles(
      final List<Path> files,
      final int index,
      final List<String> sections,
      final List<String> headings,
      final ExceptionalListener listener
  ) {
    if (index >= files.size()) {
      return ExceptionalResponse.success(new GuardrailSections(List.copyOf(sections), List.copyOf(headings)));
    }

    final Path file = files.get(index);
    return ExceptionalSupport.supply(() -> Files.readString(file), listener)
        .chain((fileListener, content) -> {
          sections.add(content);
          headings.add(fileNameStem(file));
          return readGuardrailFiles(files, index + 1, sections, headings, listener);
        }, listener);
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

  private static ExceptionalResponse<List<Path>> listMarkdownFiles(
      final Path directory,
      final ExceptionalListener listener
  ) {
    return ExceptionalSupport.supply(() -> {
      try (Stream<Path> paths = Files.list(directory)) {
        return paths
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".md"))
            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
            .toList();
      }
    }, listener);
  }

  private static String fileNameStem(final Path file) {
    final String name = file.getFileName().toString();
    return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
  }

  private static ExceptionalResponse<String> formatBundledGuardrails(final ExceptionalListener listener) {
    return readBundledResources(BUNDLED_RESOURCES, 0, new ArrayList<>(), new ArrayList<>(), listener)
        .chain((readListener, sections) -> ExceptionalResponse.success(
            joinSections(sections.sections(), sections.headings())
        ), listener);
  }

  private static ExceptionalResponse<GuardrailSections> readBundledResources(
      final List<String> resourcePaths,
      final int index,
      final List<String> sections,
      final List<String> headings,
      final ExceptionalListener listener
  ) {
    if (index >= resourcePaths.size()) {
      return ExceptionalResponse.success(new GuardrailSections(List.copyOf(sections), List.copyOf(headings)));
    }

    final String resourcePath = resourcePaths.get(index);
    return loadBundledResource(resourcePath, listener)
        .chain((resourceListener, content) -> {
          sections.add(content);
          headings.add(headingFromResource(resourcePath));
          return readBundledResources(resourcePaths, index + 1, sections, headings, listener);
        }, listener);
  }

  private static ExceptionalResponse<String> loadBundledResource(
      final String resourcePath,
      final ExceptionalListener listener
  ) {
    return ExceptionalResource.of(
        () -> openBundledStream(resourcePath),
        input -> new String(input.readAllBytes(), StandardCharsets.UTF_8)
    ).execute()
        .chain((readListener, content) -> ExceptionalResponse.success(content), listener);
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