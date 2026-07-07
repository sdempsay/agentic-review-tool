package org.dempsay.codereview.review;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;

final class RepoSummarizeFixture {

  private static final Pattern AGENT_SECTION = Pattern.compile("^=== (.+) ===$", Pattern.MULTILINE);

  private RepoSummarizeFixture() {
  }

  static List<ChangedFile> changedFiles() throws IOException {
    final List<ChangedFile> files = new ArrayList<>();
    for (final String path : readLines("/fixtures/repo-summarize/reviewable-paths.txt")) {
      if (path.endsWith(".md")) {
        files.add(ChangedFile.skipped(path, ChangeType.EXISTING, "Excluded file type (.md)"));
      } else {
        files.add(ChangedFile.included(path, ChangeType.EXISTING, "class Fixture {}\n"));
      }
    }
    return List.copyOf(files);
  }

  static List<ReviewResult> agentResults() throws IOException {
    final String findings = readResource("/fixtures/repo-summarize/agent-findings.txt");
    final Matcher matcher = AGENT_SECTION.matcher(findings);
    final List<ReviewResult> results = new ArrayList<>();
    int sectionStart = -1;
    String currentAgent = null;

    while (matcher.find()) {
      if (currentAgent != null) {
        results.add(new ReviewResult(currentAgent, findings.substring(sectionStart, matcher.start()).trim()));
      }
      currentAgent = matcher.group(1);
      sectionStart = matcher.end();
    }

    if (currentAgent != null) {
      results.add(new ReviewResult(currentAgent, findings.substring(sectionStart).trim()));
    }
    return List.copyOf(results);
  }

  private static List<String> readLines(final String resourcePath) throws IOException {
    return readResource(resourcePath).lines()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .toList();
  }

  private static String readResource(final String resourcePath) throws IOException {
    try (InputStream input = RepoSummarizeFixture.class.getResourceAsStream(resourcePath)) {
      if (input == null) {
        throw new IllegalStateException("Missing classpath resource: " + resourcePath);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}