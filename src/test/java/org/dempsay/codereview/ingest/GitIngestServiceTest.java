package org.dempsay.codereview.ingest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.junit.Test;

public class GitIngestServiceTest {

  @Test
  public void ingestUncommittedIncludesDiffForModifiedTrackedFile() throws Exception {
    final Path repoRoot = initGitRepo();
    final Path tracked = repoRoot.resolve("tracked.txt");
    Files.writeString(tracked, "initial\n");
    runGit(repoRoot, "add", "tracked.txt");
    runGit(repoRoot, "commit", "-m", "initial");
    Files.writeString(tracked, "modified\n");

    final var files = ExceptionalSupport.response(
        GitIngestService.ingest(IngestRequest.uncommitted(repoRoot, 512))
    );

    assertEquals(1, files.size());
    assertEquals("tracked.txt", files.get(0).path());
    assertEquals(ChangeType.MODIFIED, files.get(0).changeType());
    assertTrue(files.get(0).hasDiff());
    assertTrue(files.get(0).diff().contains("modified"));
  }

  @Test
  public void ingestStagedOnlyUsesCachedDiff() throws Exception {
    final Path repoRoot = initGitRepo();
    final Path tracked = repoRoot.resolve("tracked.txt");
    Files.writeString(tracked, "initial\n");
    runGit(repoRoot, "add", "tracked.txt");
    runGit(repoRoot, "commit", "-m", "initial");

    Files.writeString(tracked, "unstaged\n");
    final Path stagedOnly = repoRoot.resolve("staged.txt");
    Files.writeString(stagedOnly, "staged-content\n");
    runGit(repoRoot, "add", "staged.txt");

    final var files = ExceptionalSupport.response(
        GitIngestService.ingest(IngestRequest.staged(repoRoot, 512))
    );

    assertEquals(1, files.size());
    assertEquals("staged.txt", files.get(0).path());
    assertTrue(files.get(0).diff().contains("staged-content"));
  }

  @Test
  public void ingestIncludesUntrackedFileAsAdded() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.writeString(repoRoot.resolve("New.java"), "class New {}\n");

    final var files = ExceptionalSupport.response(
        GitIngestService.ingest(IngestRequest.uncommitted(repoRoot, 512))
    );

    assertEquals(1, files.size());
    assertEquals("New.java", files.get(0).path());
    assertEquals(ChangeType.ADDED, files.get(0).changeType());
    assertTrue(files.get(0).hasDiff());
  }

  @Test
  public void ingestAgainstBaseRef() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.writeString(repoRoot.resolve("base.txt"), "base\n");
    runGit(repoRoot, "add", "base.txt");
    runGit(repoRoot, "commit", "-m", "base");
    Files.writeString(repoRoot.resolve("feature.txt"), "feature\n");
    runGit(repoRoot, "add", "feature.txt");
    runGit(repoRoot, "commit", "-m", "feature");

    final var files = ExceptionalSupport.response(
        GitIngestService.ingest(IngestRequest.againstBase(repoRoot, "HEAD~1", 512))
    );

    assertEquals(1, files.size());
    assertEquals("feature.txt", files.get(0).path());
    assertEquals(ChangeType.ADDED, files.get(0).changeType());
  }

  @Test
  public void skipDiffLargerThanMaxDiffKb() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.writeString(repoRoot.resolve("large.txt"), "x".repeat(3000));

    final var files = ExceptionalSupport.response(
        GitIngestService.ingest(IngestRequest.uncommitted(repoRoot, 1))
    );

    assertEquals(1, files.size());
    assertFalse(files.get(0).included());
    assertTrue(files.get(0).skipReason().contains("maxDiffKb"));
  }

  @Test
  public void rejectNonGitDirectory() throws Exception {
    final Path directory = Files.createTempDirectory("not-a-git-repo");
    assertTrue(GitIngestService.ingest(IngestRequest.uncommitted(directory, 512)).wasError());
  }

  private static Path initGitRepo() throws Exception {
    final Path repoRoot = Files.createTempDirectory("code-review-ingest-repo");
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