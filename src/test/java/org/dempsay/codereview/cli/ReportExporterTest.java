package org.dempsay.codereview.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.junit.Test;

public class ReportExporterTest {

  @Test
  public void writeCreatesParentDirectoriesAndWritesMarkdown() throws Exception {
    final Path outputDir = Files.createTempDirectory("code-review-report");
    final Path outputPath = outputDir.resolve("nested/review.md");

    final Path writtenPath = ExceptionalSupport.response(ReportExporter.write(outputPath, "# Report"));

    assertEquals(outputPath.toAbsolutePath(), writtenPath);
    assertTrue(Files.isRegularFile(outputPath));
    assertEquals("# Report", Files.readString(outputPath));
  }
}