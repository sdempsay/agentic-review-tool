package org.dempsay.codereview.ingest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.junit.Test;

public class RepoIngestServiceTest {

  @Test
  public void ingestIncludesTrackedAndUntrackedNonIgnoredFiles() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.writeString(repoRoot.resolve("tracked.java"), "class Tracked {}\n");
    runGit(repoRoot, "add", "tracked.java");
    runGit(repoRoot, "commit", "-m", "tracked");
    Files.writeString(repoRoot.resolve("untracked.java"), "class Untracked {}\n");

    final var files = ExceptionalSupport.response(
        RepoIngestService.ingest(RepoIngestRequest.of(repoRoot, 512))
    );

    assertEquals(2, files.size());
    assertTrue(files.stream().anyMatch(file -> "tracked.java".equals(file.path())));
    assertTrue(files.stream().anyMatch(file -> "untracked.java".equals(file.path())));
    assertTrue(files.stream().allMatch(ChangedFile::hasDiff));
    assertTrue(files.stream().allMatch(file -> file.changeType() == ChangeType.EXISTING));
  }

  @Test
  public void ingestOmitsGitignoredUntrackedFiles() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.writeString(repoRoot.resolve(".gitignore"), "ignored.java\n");
    runGit(repoRoot, "add", ".gitignore");
    runGit(repoRoot, "commit", "-m", "gitignore");
    Files.writeString(repoRoot.resolve("ignored.java"), "class Ignored {}\n");
    Files.writeString(repoRoot.resolve("visible.java"), "class Visible {}\n");

    final var files = ExceptionalSupport.response(
        RepoIngestService.ingest(new RepoIngestRequest(repoRoot, 512, List.of(), List.of(".java"), List.of(), List.of()))
    );

    assertEquals(1, files.size());
    assertEquals("visible.java", files.get(0).path());
    assertFalse(files.stream().anyMatch(file -> "ignored.java".equals(file.path())));
  }

  @Test
  public void ingestAppliesConfigExcludeExtensions() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.writeString(repoRoot.resolve("App.java"), "class App {}\n");
    Files.writeString(repoRoot.resolve("pom.xml"), "<project/>\n");
    Files.writeString(repoRoot.resolve("config.yaml"), "key: value\n");

    final var files = ExceptionalSupport.response(
        RepoIngestService.ingest(
            new RepoIngestRequest(repoRoot, 512, List.of(), List.of(), List.of(".xml", ".yaml"), List.of())
        )
    );

    assertEquals(1, files.size());
    assertEquals("App.java", files.get(0).path());
  }

  @Test
  public void ingestExcludesMarkdownAndJsonByDefault() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.writeString(repoRoot.resolve("README.md"), "# Docs\n");
    Files.writeString(repoRoot.resolve("config.json"), "{}\n");
    Files.writeString(repoRoot.resolve("App.java"), "class App {}\n");

    final var files = ExceptionalSupport.response(
        RepoIngestService.ingest(RepoIngestRequest.of(repoRoot, 512))
    );

    assertEquals(1, files.size());
    assertEquals("App.java", files.get(0).path());
  }

  @Test
  public void ingestRespectsPathGlob() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.createDirectories(repoRoot.resolve("src"));
    Files.writeString(repoRoot.resolve("src/App.java"), "class App {}\n");
    Files.writeString(repoRoot.resolve("other.java"), "class Other {}\n");

    final var files = ExceptionalSupport.response(
        RepoIngestService.ingest(new RepoIngestRequest(repoRoot, 512, List.of("src/**"), List.of(), List.of(), List.of()))
    );

    assertEquals(1, files.size());
    assertEquals("src/App.java", files.get(0).path());
  }

  @Test
  public void ingestSkipsFileLargerThanMaxFileKb() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.writeString(repoRoot.resolve("large.java"), "x".repeat(3000));

    final var files = ExceptionalSupport.response(
        RepoIngestService.ingest(RepoIngestRequest.of(repoRoot, 1))
    );

    assertEquals(1, files.size());
    assertFalse(files.get(0).included());
    assertTrue(files.get(0).skipReason().contains("maxDiffKb"));
  }

  @Test
  public void rejectNonGitDirectory() throws Exception {
    final Path directory = Files.createTempDirectory("not-a-git-repo");
    assertTrue(RepoIngestService.ingest(RepoIngestRequest.of(directory, 512)).wasError());
  }

  private static Path initGitRepo() throws Exception {
    final Path repoRoot = Files.createTempDirectory("code-review-repo-ingest");
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