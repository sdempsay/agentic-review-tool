package org.dempsay.codereview.ingest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class GitRunner {

  private GitRunner() {
  }

  static boolean isGitRepository(final Path repoRoot) {
    return Files.isDirectory(repoRoot.resolve(".git"));
  }

  static boolean hasCommits(final Path repoRoot) throws IOException, InterruptedException {
    return run(repoRoot, "rev-parse", "HEAD").exitCode() == 0;
  }

  static GitResult run(final Path repoRoot, final String... command) throws IOException, InterruptedException {
    final List<String> gitCommand = new ArrayList<>();
    gitCommand.add("git");
    for (final String argument : command) {
      gitCommand.add(argument);
    }

    final ProcessBuilder processBuilder = new ProcessBuilder(gitCommand);
    processBuilder.directory(repoRoot.toFile());
    processBuilder.redirectErrorStream(true);

    final Process process = processBuilder.start();
    final StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!output.isEmpty()) {
          output.append(System.lineSeparator());
        }
        output.append(line);
      }
    }
    final int exitCode = process.waitFor();
    return new GitResult(exitCode, output.toString());
  }

  static List<String> runLines(final Path repoRoot, final String... command)
      throws IOException, InterruptedException {
    final GitResult result = run(repoRoot, command);
    if (result.exitCode() != 0) {
      throw new IllegalStateException(
          "git " + String.join(" ", command) + " failed with exit code " + result.exitCode()
      );
    }
    if (result.output().isBlank()) {
      return List.of();
    }
    final List<String> lines = new ArrayList<>();
    for (final String line : result.output().split(System.lineSeparator())) {
      if (!line.isBlank()) {
        lines.add(line.trim());
      }
    }
    return lines;
  }

  record GitResult(int exitCode, String output) {
  }
}