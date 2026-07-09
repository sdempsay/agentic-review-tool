package org.dempsay.codereview.ingest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class GitRunnerTest {

  @Test
  public void isGitRepositoryAcceptsDotGitDirectory() throws Exception {
    final Path root = Files.createTempDirectory("git-dir-repo");
    Files.createDirectory(root.resolve(".git"));
    assertTrue(GitRunner.isGitRepository(root));
  }

  @Test
  public void isGitRepositoryAcceptsDotGitFile() throws Exception {
    final Path root = Files.createTempDirectory("git-file-repo");
    Files.writeString(root.resolve(".git"), "gitdir: ../.git/modules/example\n");
    assertTrue(GitRunner.isGitRepository(root));
  }

  @Test
  public void isGitRepositoryRejectsMissingDotGit() throws Exception {
    final Path root = Files.createTempDirectory("not-git");
    assertFalse(GitRunner.isGitRepository(root));
  }
}