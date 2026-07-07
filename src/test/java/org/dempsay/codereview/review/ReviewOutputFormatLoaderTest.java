package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.junit.Test;

public class ReviewOutputFormatLoaderTest {

  @Test
  public void loadUsesRulesDirOverrideWhenPresent() throws Exception {
    final Path rulesDir = Files.createTempDirectory("code-review-output-format");
    Files.writeString(
        rulesDir.resolve(ReviewOutputFormatLoader.FILE_NAME),
        "## Custom Output\n- custom bullet"
    );

    final String body = ExceptionalSupport.response(ReviewOutputFormatLoader.load(rulesDir));

    assertTrue(body.contains("Custom Output"));
    assertTrue(body.contains("custom bullet"));
  }

  @Test
  public void loadFallsBackToBundledWhenRulesDirMissingFile() {
    final String body = ExceptionalSupport.response(ReviewOutputFormatLoader.load(null));

    assertTrue(body.contains("## Output"));
    assertTrue(body.contains("insufficient context"));
  }

  @Test
  public void fileNameIsStable() {
    assertEquals("review-output-format.md", ReviewOutputFormatLoader.FILE_NAME);
  }
}