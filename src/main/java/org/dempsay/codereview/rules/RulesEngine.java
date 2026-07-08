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
    if (rulesDir != null && Files.isDirectory(rulesDir)) {
      return loadFromDirectory(rulesDir, listener);
    }
    return loadBundledRules(listener);
  }

  private static ExceptionalResponse<List<Rule>> loadFromDirectory(
      final Path rulesDir,
      final ExceptionalListener listener
  ) {
    return listRuleFiles(rulesDir)
        .chain((listListener, ruleFiles) ->
            loadRulesFromPaths(ruleFiles, 0, new ArrayList<>(), listener),
            listener
        );
  }

  private static ExceptionalResponse<List<Path>> listRuleFiles(final Path rulesDir) {
    return ExceptionalSupport.supply(() -> {
      try (Stream<Path> files = Files.list(rulesDir)) {
        return files
            .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md"))
            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
            .toList();
      }
    });
  }

  private static ExceptionalResponse<List<Rule>> loadRulesFromPaths(
      final List<Path> ruleFiles,
      final int index,
      final List<Rule> rules,
      final ExceptionalListener listener
  ) {
    if (index >= ruleFiles.size()) {
      return ExceptionalResponse.success(List.copyOf(rules));
    }

    final Path ruleFile = ruleFiles.get(index);
    return hasRuleFrontmatter(ruleFile, listener)
        .chain((frontmatterListener, hasFrontmatter) -> {
          if (!hasFrontmatter) {
            return loadRulesFromPaths(ruleFiles, index + 1, rules, listener);
          }
          return loadRule(ruleFile, frontmatterListener)
              .chain((ruleListener, rule) -> {
                rules.add(rule);
                return loadRulesFromPaths(ruleFiles, index + 1, rules, listener);
              }, listener);
        }, listener);
  }

  private static ExceptionalResponse<Boolean> hasRuleFrontmatter(
      final Path ruleFile,
      final ExceptionalListener listener
  ) {
    return ExceptionalSupport.supply(() -> Files.readString(ruleFile), listener)
        .chain((readListener, content) -> {
          final String normalized = content.startsWith("\uFEFF") ? content.substring(1) : content;
          return ExceptionalResponse.success(normalized.startsWith("---"));
        }, listener);
  }

  private static ExceptionalResponse<List<Rule>> loadBundledRules(final ExceptionalListener listener) {
    return loadBundledRulesFromResources(BUNDLED_RULE_RESOURCES, 0, new ArrayList<>(), listener);
  }

  private static ExceptionalResponse<List<Rule>> loadBundledRulesFromResources(
      final List<String> resourcePaths,
      final int index,
      final List<Rule> rules,
      final ExceptionalListener listener
  ) {
    if (index >= resourcePaths.size()) {
      return ExceptionalResponse.success(List.copyOf(rules));
    }

    return loadRuleFromClasspath(resourcePaths.get(index), listener)
        .chain((ruleListener, rule) -> {
          rules.add(rule);
          return loadBundledRulesFromResources(resourcePaths, index + 1, rules, listener);
        }, listener);
  }

  static ExceptionalResponse<Rule> loadRule(final Path ruleFile) {
    return loadRule(ruleFile, null);
  }

  static ExceptionalResponse<Rule> loadRule(final Path ruleFile, final ExceptionalListener listener) {
    return ExceptionalResource.of(
        () -> Files.newBufferedReader(ruleFile),
        reader -> reader.lines().collect(Collectors.joining(System.lineSeparator()))
    ).execute()
        .chain((readListener, content) -> toRule(
            stripExtension(ruleFile.getFileName().toString()),
            ruleFile,
            content,
            readListener
        ), listener);
  }

  static ExceptionalResponse<Rule> loadRuleFromClasspath(final String resourcePath) {
    return loadRuleFromClasspath(resourcePath, null);
  }

  static ExceptionalResponse<Rule> loadRuleFromClasspath(
      final String resourcePath,
      final ExceptionalListener listener
  ) {
    return ExceptionalResource.of(
        () -> openBundledRuleStream(resourcePath),
        input -> new String(input.readAllBytes())
    ).execute()
        .chain((readListener, content) -> {
          final String fileName = Path.of(resourcePath).getFileName().toString();
          return toRule(stripExtension(fileName), null, content, readListener);
        }, listener);
  }

  private static ExceptionalResponse<Rule> toRule(
      final String id,
      final Path sourceFile,
      final String content,
      final ExceptionalListener listener
  ) {
    return FrontmatterParser.parse(content)
        .chain((parseListener, document) -> ExceptionalResponse.success(
            new Rule(id, sourceFile, List.copyOf(document.pathGlobs()), document.promptBody())
        ), listener);
  }

  private static InputStream openBundledRuleStream(final String resourcePath) {
    final InputStream input = RulesEngine.class.getResourceAsStream(resourcePath);
    if (input == null) {
      throw new IllegalStateException("Bundled rule is missing: " + resourcePath);
    }
    return input;
  }

  private static String stripExtension(final String fileName) {
    final int dot = fileName.lastIndexOf('.');
    return dot > 0 ? fileName.substring(0, dot) : fileName;
  }
}