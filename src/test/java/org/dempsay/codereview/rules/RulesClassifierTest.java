package org.dempsay.codereview.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public class RulesClassifierTest {

  @Test
  public void matchesJavaFilesAgainstJavaGlob() {
    final List<Rule> rules = List.of(
        new Rule("java-general", Path.of("java-general.md"), List.of("**/*.java"), "body")
    );

    final List<Rule> matched = RulesClassifier.classifyFile(rules, "src/main/java/Foo.java");

    assertEquals(1, matched.size());
    assertEquals("java-general", matched.get(0).id());
  }

  @Test
  public void multipleRulesCanMatchSameFile() {
    final List<Rule> rules = List.of(
        new Rule("java-general", Path.of("java-general.md"), List.of("**/*.java"), "body"),
        new Rule("java-formatting", Path.of("java-formatting.md"), List.of("**/*.java"), "body"),
        new Rule("service-only", Path.of("service.md"), List.of("**/service/**"), "body")
    );

    final List<Rule> matched = RulesClassifier.classifyFile(rules, "src/main/java/service/UserService.java");

    assertEquals(3, matched.size());
    assertEquals("java-general", matched.get(0).id());
    assertEquals("java-formatting", matched.get(1).id());
    assertEquals("service-only", matched.get(2).id());
  }

  @Test
  public void matchesRootPomXmlAgainstPomSecurityGlob() {
    final List<Rule> rules = List.of(
        new Rule("pom-security", Path.of("pom-security.md"), List.of("pom.xml", "**/pom.xml"), "body")
    );

    final List<Rule> matched = RulesClassifier.classifyFile(rules, "pom.xml");

    assertEquals(1, matched.size());
    assertEquals("pom-security", matched.get(0).id());
  }

  @Test
  public void nonMatchingFilesReturnEmptyRuleList() {
    final List<Rule> rules = List.of(
        new Rule("java-general", Path.of("java-general.md"), List.of("**/*.java"), "body")
    );

    final var matches = RulesClassifier.classify(rules, List.of("README.md", "src/main/java/App.java"));

    assertTrue(matches.get("README.md").isEmpty());
    assertEquals(1, matches.get("src/main/java/App.java").size());
  }
}