package org.dempsay.codereview.rules;

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
      "/rules/java-exceptional.md",
      "/rules/java-formatting.md",
      "/rules/java-javadoc.md",
      "/rules/pom-security.md",
      "/rules/xml-formatter.md"
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
    return ExceptionalSupport.supply(() -> {
      final List<Rule> fromDirectory = loadFromDirectory(rulesDir);
      if (!fromDirectory.isEmpty()) {
        return fromDirectory;
      }
      return loadBundledRules();
    }, listener);
  }

  private static List<Rule> loadFromDirectory(final Path rulesDir) {
    if (rulesDir == null || !Files.isDirectory(rulesDir)) {
      return List.of();
    }

    final List<Path> ruleFiles = ExceptionalSupport.response(ExceptionalSupport.supply(() -> {
      try (Stream<Path> files = Files.list(rulesDir)) {
        return files
            .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md"))
            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
            .toList();
      }
    }));

    final List<Rule> rules = new ArrayList<>();
    for (final Path ruleFile : ruleFiles) {
      if (!hasRuleFrontmatter(ruleFile)) {
        continue;
      }
      rules.add(ExceptionalSupport.response(loadRule(ruleFile)));
    }
    return rules;
  }

  private static List<Rule> loadBundledRules() {
    final List<Rule> rules = new ArrayList<>();
    for (final String resourcePath : BUNDLED_RULE_RESOURCES) {
      rules.add(ExceptionalSupport.response(loadRuleFromClasspath(resourcePath)));
    }
    return List.copyOf(rules);
  }

  static ExceptionalResponse<Rule> loadRule(final Path ruleFile) {
    return ExceptionalResource.of(
        () -> Files.newBufferedReader(ruleFile),
        reader -> reader.lines().collect(Collectors.joining(System.lineSeparator()))
    ).execute()
        .chain((listener, content) -> {
          final FrontmatterParser.ParsedRuleDocument document =
              ExceptionalSupport.response(FrontmatterParser.parse(content));
          final String id = stripExtension(ruleFile.getFileName().toString());
          return ExceptionalResponse.success(
              new Rule(id, ruleFile, List.copyOf(document.pathGlobs()), document.promptBody())
          );
        });
  }

  static ExceptionalResponse<Rule> loadRuleFromClasspath(final String resourcePath) {
    return ExceptionalResource.of(
        () -> openBundledRuleStream(resourcePath),
        input -> new String(input.readAllBytes())
    ).execute()
        .chain((listener, content) -> {
          final FrontmatterParser.ParsedRuleDocument document =
              ExceptionalSupport.response(FrontmatterParser.parse(content));
          final String fileName = Path.of(resourcePath).getFileName().toString();
          final String id = stripExtension(fileName);
          return ExceptionalResponse.success(
              new Rule(id, null, List.copyOf(document.pathGlobs()), document.promptBody())
          );
        });
  }

  private static InputStream openBundledRuleStream(final String resourcePath) {
    final InputStream input = RulesEngine.class.getResourceAsStream(resourcePath);
    if (input == null) {
      throw new IllegalStateException("Bundled rule is missing: " + resourcePath);
    }
    return input;
  }

  private static boolean hasRuleFrontmatter(final Path ruleFile) {
    final String content = ExceptionalSupport.response(ExceptionalSupport.supply(() -> Files.readString(ruleFile)));
    final String normalized = content.startsWith("\uFEFF") ? content.substring(1) : content;
    return normalized.startsWith("---");
  }

  private static String stripExtension(final String fileName) {
    final int dot = fileName.lastIndexOf('.');
    return dot > 0 ? fileName.substring(0, dot) : fileName;
  }
}