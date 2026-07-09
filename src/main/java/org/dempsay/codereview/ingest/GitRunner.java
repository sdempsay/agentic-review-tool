package org.dempsay.codereview.ingest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

final class GitRunner {

  private GitRunner() {
  }

  static boolean isGitRepository(final Path repoRoot) {
    final Path dotGit = repoRoot.resolve(".git");
    // Submodules and linked worktrees use a .git file (gitdir: ...) instead of a .git directory.
    return Files.isDirectory(dotGit) || Files.isRegularFile(dotGit);
  }

  static ExceptionalResponse<Boolean> hasCommits(final Path repoRoot) {
    return hasCommits(repoRoot, null);
  }

  static ExceptionalResponse<Boolean> hasCommits(final Path repoRoot, final ExceptionalListener listener) {
    return run(repoRoot, listener, "rev-parse", "HEAD")
        .chain((runListener, result) -> ExceptionalResponse.success(result.exitCode() == 0), listener);
  }

  static ExceptionalResponse<GitResult> run(final Path repoRoot, final String... command) {
    return run(repoRoot, null, command);
  }

  static ExceptionalResponse<GitResult> run(
      final Path repoRoot,
      final ExceptionalListener listener,
      final String... command
  ) {
    return ExceptionalSupport.supply(() -> {
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
    }, listener);
  }

  static ExceptionalResponse<List<String>> runLines(final Path repoRoot, final String... command) {
    return runLines(repoRoot, null, command);
  }

  static ExceptionalResponse<List<String>> runLines(
      final Path repoRoot,
      final ExceptionalListener listener,
      final String... command
  ) {
    return run(repoRoot, listener, command)
        .chain((runListener, result) -> {
          if (result.exitCode() != 0) {
            return ExceptionalSupport.fail(
                runListener,
                new IllegalStateException(
                    "git " + String.join(" ", command) + " failed with exit code " + result.exitCode()
                )
            );
          }
          if (result.output().isBlank()) {
            return ExceptionalResponse.success(List.of());
          }
          final List<String> lines = new ArrayList<>();
          for (final String line : result.output().split(System.lineSeparator())) {
            if (!line.isBlank()) {
              lines.add(line.trim());
            }
          }
          return ExceptionalResponse.success(List.copyOf(lines));
        }, listener);
  }

  record GitResult(int exitCode, String output) {
  }
}