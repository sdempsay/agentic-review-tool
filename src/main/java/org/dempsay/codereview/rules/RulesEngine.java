package org.dempsay.codereview.rules;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResource;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class RulesEngine {

  private static final List<String> BUNDLED_RULE_RESOURCES = List.of(
      "/rules/java-general.md",
      "/rules/java-formatting.md"
  );

  private RulesEngine() {
  }

  public static ExceptionalResponse<List<Rule>> load(final Path rulesDir) {
    return load(rulesDir, null);
  }

  public static ExceptionalResponse<List<Rule>> load(
      final Path rulesDir,
      final ExceptionalListener listener
  ) {
    return ExceptionalSupport.supply(() -> loadRequired(rulesDir), listener);
  }

  public static List<Rule> loadRequired(final Path rulesDir) throws IOException {
    final List<Rule> fromDirectory = loadFromDirectory(rulesDir);
    if (!fromDirectory.isEmpty()) {
      return fromDirectory;
    }
    return loadBundledRules();
  }

  private static List<Rule> loadFromDirectory(final Path rulesDir) throws IOException {
    if (rulesDir == null || !Files.isDirectory(rulesDir)) {
      return List.of();
    }

    final List<Rule> rules = new ArrayList<>();
    try (Stream<Path> files = Files.list(rulesDir)) {
      final List<Path> ruleFiles = files
          .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md"))
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .toList();
      for (final Path ruleFile : ruleFiles) {
        if (!hasRuleFrontmatter(ruleFile)) {
          continue;
        }
        rules.add(loadRule(ruleFile));
      }
    }
    return rules;
  }

  private static List<Rule> loadBundledRules() throws IOException {
    final List<Rule> rules = new ArrayList<>();
    for (final String resourcePath : BUNDLED_RULE_RESOURCES) {
      rules.add(loadRuleFromClasspath(resourcePath));
    }
    return List.copyOf(rules);
  }

  static Rule loadRule(final Path ruleFile) throws IOException {
    final ExceptionalResponse<String> response = ExceptionalResource.of(
        () -> Files.newBufferedReader(ruleFile),
        reader -> reader.lines().collect(Collectors.joining(System.lineSeparator()))
    ).execute();
    if (response.wasError()) {
      throw new IOException("Failed to read rule file " + ruleFile);
    }
    final FrontmatterParser.ParsedRuleDocument document =
        FrontmatterParser.parseRequired(response.response());
    final String id = stripExtension(ruleFile.getFileName().toString());
    return new Rule(id, ruleFile, List.copyOf(document.pathGlobs()), document.promptBody());
  }

  static Rule loadRuleFromClasspath(final String resourcePath) throws IOException {
    final ExceptionalResponse<String> response = ExceptionalResource.of(
        () -> openBundledRuleStream(resourcePath),
        input -> new String(input.readAllBytes())
    ).execute();
    if (response.wasError()) {
      throw new IOException("Failed to read bundled rule " + resourcePath);
    }
    final FrontmatterParser.ParsedRuleDocument document =
        FrontmatterParser.parseRequired(response.response());
    final String fileName = Path.of(resourcePath).getFileName().toString();
    final String id = stripExtension(fileName);
    return new Rule(id, null, List.copyOf(document.pathGlobs()), document.promptBody());
  }

  private static InputStream openBundledRuleStream(final String resourcePath) {
    final InputStream input = RulesEngine.class.getResourceAsStream(resourcePath);
    if (input == null) {
      throw new IllegalStateException("Bundled rule is missing: " + resourcePath);
    }
    return input;
  }

  private static boolean hasRuleFrontmatter(final Path ruleFile) throws IOException {
    final String content = Files.readString(ruleFile);
    final String normalized = content.startsWith("\uFEFF") ? content.substring(1) : content;
    return normalized.startsWith("---");
  }

  private static String stripExtension(final String fileName) {
    final int dot = fileName.lastIndexOf('.');
    return dot > 0 ? fileName.substring(0, dot) : fileName;
  }
}