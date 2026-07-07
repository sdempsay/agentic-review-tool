package org.dempsay.codereview.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.Test;

public class RepoCommandTest {

  @Test
  public void dryRunWritesMarkdownReportWithScopeAndClassification() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.createDirectories(repoRoot.resolve("src"));
    Files.writeString(repoRoot.resolve("src/App.java"), "class App {}\n");

    final Path rulesDir = copyBundledRules();
    final Path config = writeConfig(rulesDir);
    final Path output = repoRoot.resolve("report.md");

    final int exitCode = runInRepo(
        repoRoot,
        "repo",
        "--dry-run",
        "--no-chat",
        "--quiet",
        "--config",
        config.toAbsolutePath().toString(),
        "--output",
        output.toAbsolutePath().toString()
    );

    assertEquals(0, exitCode);
    assertTrue(Files.exists(output));
    final String report = Files.readString(output);
    assertTrue(report.contains("# Code Review Report"));
    assertTrue(report.contains("**Scope:** repository (tracked + untracked)"));
    assertTrue(report.contains("`src/App.java`"));
    assertTrue(report.contains("java-general"));
    assertFalse(report.contains("## Review"));
  }

  @Test
  public void dryRunRespectsPathGlobInReport() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.createDirectories(repoRoot.resolve("src"));
    Files.writeString(repoRoot.resolve("src/App.java"), "class App {}\n");
    Files.writeString(repoRoot.resolve("other.java"), "class Other {}\n");

    final Path rulesDir = copyBundledRules();
    final Path config = writeConfig(rulesDir);
    final Path output = repoRoot.resolve("scoped-report.md");

    final int exitCode = runInRepo(
        repoRoot,
        "repo",
        "--dry-run",
        "--no-chat",
        "--quiet",
        "--config",
        config.toAbsolutePath().toString(),
        "--path",
        "src/**",
        "--output",
        output.toAbsolutePath().toString()
    );

    assertEquals(0, exitCode);
    final String report = Files.readString(output);
    assertTrue(report.contains("**Scope:** repository (tracked + untracked); --path src/**"));
    assertTrue(report.contains("`src/App.java`"));
    assertFalse(report.contains("`other.java`"));
  }

  @Test
  public void dryRunRespectsExcludeExtensionInReport() throws Exception {
    final Path repoRoot = initGitRepo();
    Files.writeString(repoRoot.resolve("App.java"), "class App {}\n");
    Files.writeString(repoRoot.resolve("pom.xml"), "<project/>");

    final Path rulesDir = copyBundledRules();
    final Path config = writeConfig(rulesDir);
    final Path output = repoRoot.resolve("exclude-ext-report.md");

    final int exitCode = runInRepo(
        repoRoot,
        "repo",
        "--dry-run",
        "--no-chat",
        "--quiet",
        "--config",
        config.toAbsolutePath().toString(),
        "--exclude-ext",
        ".xml",
        "--output",
        output.toAbsolutePath().toString()
    );

    assertEquals(0, exitCode);
    final String report = Files.readString(output);
    assertTrue(report.contains("--exclude-ext .xml"));
    assertTrue(report.contains("`App.java`"));
    assertFalse(report.contains("`pom.xml`"));
  }

  private static int runInRepo(final Path repoRoot, final String... args) throws Exception {
    final String[] command = new String[args.length + 4];
    command[0] = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    command[1] = "-cp";
    command[2] = System.getProperty("java.class.path");
    command[3] = "org.dempsay.codereview.CodeReviewApplication";
    System.arraycopy(args, 0, command, 4, args.length);

    final ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.directory(repoRoot.toFile());
    processBuilder.redirectErrorStream(true);
    final Process process = processBuilder.start();
    return process.waitFor();
  }

  private static Path writeConfig(final Path rulesDir) throws Exception {
    final Path config = Files.createTempFile("code-review-repo-config", ".json");
    Files.writeString(
        config,
        """
        {
          "model": {
            "provider": "ollama",
            "name": "test-model",
            "temperature": 0.2,
            "baseUrl": "http://127.0.0.1:11434",
            "timeoutSeconds": 30
          },
          "rulesDir": "%s",
          "maxTokens": 4096,
          "maxDiffKb": 512
        }
        """.formatted(rulesDir.toString().replace("\\", "\\\\"))
    );
    return config;
  }

  private static Path copyBundledRules() throws Exception {
    final Path rulesDir = Files.createTempDirectory("code-review-repo-rules");
    copyResource("/rules/java-general.md", rulesDir.resolve("java-general.md"));
    copyResource("/rules/java-formatting.md", rulesDir.resolve("java-formatting.md"));
    return rulesDir;
  }

  private static void copyResource(final String resourcePath, final Path destination) throws Exception {
    try (InputStream input = RepoCommandTest.class.getResourceAsStream(resourcePath)) {
      if (input == null) {
        throw new IllegalStateException("Missing classpath resource: " + resourcePath);
      }
      Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static Path initGitRepo() throws Exception {
    final Path repoRoot = Files.createTempDirectory("code-review-repo-command");
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