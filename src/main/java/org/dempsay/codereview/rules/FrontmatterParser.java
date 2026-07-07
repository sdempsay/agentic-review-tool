package org.dempsay.codereview.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.yaml.snakeyaml.Yaml;

public final class FrontmatterParser {

  private static final Yaml YAML = new Yaml();

  private FrontmatterParser() {
  }

  public static ExceptionalResponse<ParsedRuleDocument> parse(final String content) {
    return ExceptionalSupport.supply(() -> parseRequired(content));
  }

  static ParsedRuleDocument parseRequired(final String content) {
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("Rule file is empty");
    }
    final String normalized = content.startsWith("\uFEFF") ? content.substring(1) : content;
    if (!normalized.startsWith("---")) {
      throw new IllegalArgumentException("Rule file must begin with YAML frontmatter delimited by ---");
    }

    final int closingDelimiter = normalized.indexOf("---", 3);
    if (closingDelimiter < 0) {
      throw new IllegalArgumentException("Rule file frontmatter is missing closing --- delimiter");
    }

    final String frontmatter = normalized.substring(3, closingDelimiter).trim();
    final String body = normalized.substring(closingDelimiter + 3).trim();
    final List<String> paths = parsePaths(frontmatter);
    if (paths.isEmpty()) {
      throw new IllegalArgumentException("Rule frontmatter must include at least one paths glob");
    }
    return new ParsedRuleDocument(paths, body);
  }

  @SuppressWarnings("unchecked")
  private static List<String> parsePaths(final String frontmatter) {
    final Object loaded = YAML.load(frontmatter);
    if (!(loaded instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException("Rule frontmatter must be a YAML map");
    }
    final Object pathsNode = map.get("paths");
    if (!(pathsNode instanceof List<?> pathList)) {
      throw new IllegalArgumentException("Rule frontmatter must define paths as a list of globs");
    }

    final List<String> paths = new ArrayList<>();
    for (final Object entry : pathList) {
      if (!(entry instanceof String glob) || glob.isBlank()) {
        throw new IllegalArgumentException("Each paths entry must be a non-blank glob string");
      }
      paths.add(glob.trim());
    }
    return paths;
  }

  public record ParsedRuleDocument(List<String> pathGlobs, String promptBody) {
  }
}