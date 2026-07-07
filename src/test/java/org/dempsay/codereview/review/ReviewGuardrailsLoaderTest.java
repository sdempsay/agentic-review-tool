package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.junit.Test;

public class ReviewGuardrailsLoaderTest {

  @Test
  public void loadUsesGuardrailsDirectoryWhenPresent() throws Exception {
    final Path rulesDir = Files.createTempDirectory("code-review-guardrails");
    final Path guardrailsDir = rulesDir.resolve(ReviewGuardrailsLoader.SUBDIRECTORY);
    Files.createDirectories(guardrailsDir);
    Files.writeString(guardrailsDir.resolve("custom.md"), "- custom guardrail");

    final String body = ExceptionalSupport.response(ReviewGuardrailsLoader.load(rulesDir));

    assertTrue(body.contains("## Guardrails"));
    assertTrue(body.contains("### custom"));
    assertTrue(body.contains("custom guardrail"));
    assertFalse(body.contains("no-internet"));
  }

  @Test
  public void loadReturnsEmptyWhenGuardrailsDirectoryExistsButHasNoMarkdown() throws Exception {
    final Path rulesDir = Files.createTempDirectory("code-review-empty-guardrails");
    Files.createDirectories(rulesDir.resolve(ReviewGuardrailsLoader.SUBDIRECTORY));

    final String body = ExceptionalSupport.response(ReviewGuardrailsLoader.load(rulesDir));

    assertEquals("", body);
  }

  @Test
  public void loadFallsBackToBundledWhenGuardrailsDirectoryMissing() {
    final String body = ExceptionalSupport.response(ReviewGuardrailsLoader.load(null));

    assertTrue(body.contains("## Guardrails"));
    assertTrue(body.contains("### no-modify"));
    assertTrue(body.contains("### no-internet"));
    assertTrue(body.contains("Do not search the internet"));
  }
}