package org.dempsay.codereview.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.junit.Test;

public class RulesEngineTest {

  @Test
  public void loadRulesFromDirectory() throws Exception {
    final Path rulesDir = Files.createTempDirectory("code-review-rules");
    Files.writeString(
        rulesDir.resolve("custom-rule.md"),
        """
        ---
        paths:
          - "**/*.kt"
        ---

        Kotlin rules
        """
    );

    final var rules = ExceptionalSupport.response(RulesEngine.load(rulesDir));

    assertEquals(1, rules.size());
    assertEquals("custom-rule", rules.get(0).id());
    assertEquals("Kotlin rules", rules.get(0).promptBody());
  }

  @Test
  public void fallBackToBundledRulesWhenDirectoryMissing() {
    final var rules = ExceptionalSupport.response(RulesEngine.load(Path.of("/path/that/does/not/exist")));

    assertEquals(2, rules.size());
    assertEquals("java-formatting", rules.get(1).id());
    assertTrue(rules.stream().anyMatch(rule -> rule.id().equals("java-general")));
  }

  @Test
  public void skipInstructionalDocsWithoutFrontmatter() throws Exception {
    final Path rulesDir = Files.createTempDirectory("code-review-mixed-rules");
    Files.writeString(rulesDir.resolve("maven.md"), "# Maven instructions without path globs");
    Files.writeString(
        rulesDir.resolve("java.md"),
        """
        ---
        paths:
          - "**/*.java"
        ---

        Java rules
        """
    );

    final var rules = ExceptionalSupport.response(RulesEngine.load(rulesDir));

    assertEquals(1, rules.size());
    assertEquals("java", rules.get(0).id());
    assertEquals("Java rules", rules.get(0).promptBody());
  }
}