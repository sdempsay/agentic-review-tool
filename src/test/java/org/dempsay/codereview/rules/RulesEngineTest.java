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
  public void rejectMalformedRuleFile() throws Exception {
    final Path rulesDir = Files.createTempDirectory("code-review-bad-rules");
    Files.writeString(rulesDir.resolve("broken.md"), "# missing frontmatter");

    assertTrue(RulesEngine.load(rulesDir).wasError());
  }
}