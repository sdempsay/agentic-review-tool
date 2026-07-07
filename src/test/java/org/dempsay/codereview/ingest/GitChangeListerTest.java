package org.dempsay.codereview.ingest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.junit.Test;

public class GitChangeListerTest {

  @Test
  public void listUncommittedChangesIncludesStagedAndUnstagedFiles() throws Exception {
    final Path repoRoot = initGitRepo();
    final Path tracked = repoRoot.resolve("tracked.txt");
    Files.writeString(tracked, "initial\n");
    runGit(repoRoot, "add", "tracked.txt");
    runGit(repoRoot, "commit", "-m", "initial");

    Files.writeString(tracked, "modified\n");
    final Path stagedOnly = repoRoot.resolve("staged.txt");
    Files.writeString(stagedOnly, "staged\n");
    runGit(repoRoot, "add", "staged.txt");

    final var changedFiles = ExceptionalSupport.response(GitChangeLister.listUncommittedChanges(repoRoot));
    final var paths = changedFiles.stream().map(ChangedFile::path).toList();

    assertTrue(paths.contains("tracked.txt"));
    assertTrue(paths.contains("staged.txt"));
    assertEquals(2, paths.size());
  }

  @Test
  public void listUncommittedChangesIncludesUntrackedFiles() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.writeString(repoRoot.resolve("untracked.java"), "class Untracked {}\n");

    final var paths = ExceptionalSupport.response(GitChangeLister.listUncommittedChanges(repoRoot)).stream()
        .map(ChangedFile::path)
        .toList();

    assertTrue(paths.contains("untracked.java"));
  }

  @Test
  public void rejectNonGitDirectory() throws Exception {
    final Path directory = Files.createTempDirectory("not-a-git-repo");
    assertTrue(GitChangeLister.listUncommittedChanges(directory).wasError());
  }

  private static Path initGitRepo() throws Exception {
    final Path repoRoot = Files.createTempDirectory("code-review-git-repo");
    runGit(repoRoot, "init");
    runGit(repoRoot, "config", "user.email", "test@example.com");
    runGit(repoRoot, "config", "user.name", "Test User");
    return repoRoot;
  }

  private static void runGit(final Path repoRoot, final String... command) throws Exception {
    final String[] gitCommand = new String[command.length + 1];
    gitCommand[0] = "git";
    System.arraycopy(command, 0, gitCommand, 1, command.length);

    final ProcessBuilder processBuilder = new ProcessBuilder(gitCommand);
    processBuilder.directory(repoRoot.toFile());
    processBuilder.redirectErrorStream(true);
    final Process process = processBuilder.start();
    final int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IllegalStateException("git command failed: " + String.join(" ", gitCommand));
    }
  }
}