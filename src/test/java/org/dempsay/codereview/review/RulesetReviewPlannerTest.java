package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;
import org.junit.Test;

public class RulesetReviewPlannerTest {

  @Test
  public void planCreatesSeparateTaskPerMatchedRule() {
    final Rule javaRule = new Rule("java-general", null, List.of("**/*.java"), "Java rules");
    final Rule pomRule = new Rule("pom-tidy", null, List.of("**/pom.xml"), "POM rules");
    final ChangedFile javaFile = ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+java");
    final ChangedFile pomFile = ChangedFile.included("pom.xml", ChangeType.MODIFIED, "+pom");

    final List<RulesetReviewTask> tasks = RulesetReviewPlanner.plan(
        List.of(javaRule, pomRule),
        Map.of(
            "src/App.java", List.of(javaRule),
            "pom.xml", List.of(pomRule)
        ),
        List.of(javaFile, pomFile)
    );

    assertEquals(2, tasks.size());
    assertEquals("java-general", tasks.get(0).agentName());
    assertEquals(List.of(javaFile), tasks.get(0).files());
    assertEquals("pom-tidy", tasks.get(1).agentName());
    assertEquals(List.of(pomFile), tasks.get(1).files());
  }

  @Test
  public void planIncludesGeneralFallbackForUnmatchedFiles() {
    final Rule javaRule = new Rule("java-general", Path.of("java-general.md"), List.of("**/*.java"), "Java rules");
    final ChangedFile javaFile = ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+java");
    final ChangedFile readmeFile = ChangedFile.included("README.md", ChangeType.MODIFIED, "+readme");

    final List<RulesetReviewTask> tasks = RulesetReviewPlanner.plan(
        List.of(javaRule),
        Map.of("src/App.java", List.of(javaRule), "README.md", List.of()),
        List.of(javaFile, readmeFile)
    );

    assertEquals(2, tasks.size());
    assertEquals("java-general", tasks.get(0).agentName());
    assertTrue(tasks.get(1).isGeneralFallback());
    assertEquals(List.of(readmeFile), tasks.get(1).files());
  }

  @Test
  public void planSplitsOversizedRulesetIntoBatches() {
    final Rule javaRule = new Rule("java-general", null, List.of("**/*.java"), "Java rules");
    final ChangedFile first = ChangedFile.included("src/A.java", ChangeType.MODIFIED, "a".repeat(600));
    final ChangedFile second = ChangedFile.included("src/B.java", ChangeType.MODIFIED, "b".repeat(600));

    final List<RulesetReviewTask> tasks = RulesetReviewPlanner.plan(
        List.of(javaRule),
        Map.of("src/A.java", List.of(javaRule), "src/B.java", List.of(javaRule)),
        List.of(first, second),
        1,
        0
    );

    assertEquals(2, tasks.size());
    assertEquals("java-general (batch 1/2)", tasks.get(0).agentName());
    assertEquals("java-general (batch 2/2)", tasks.get(1).agentName());
    assertEquals(List.of(first), tasks.get(0).files());
    assertEquals(List.of(second), tasks.get(1).files());
  }

  @Test
  public void planKeepsSingleOversizedFileWhenHardCapAllowsIt() {
    final Rule javaRule = new Rule("java-general", null, List.of("**/*.java"), "Java rules");
    final ChangedFile large = ChangedFile.included("src/A.java", ChangeType.MODIFIED, "a".repeat(5_000));
    final org.dempsay.codereview.config.AppConfig config = new org.dempsay.codereview.config.AppConfig(
        new org.dempsay.codereview.config.ModelConfig("ollama", "qwen3", 0.2, null, 0),
        Path.of("/tmp"),
        8000,
        512,
        1,
        0,
        List.of()
    );

    final List<RulesetReviewTask> tasks = RulesetReviewPlanner.plan(
        List.of(javaRule),
        Map.of("src/A.java", List.of(javaRule)),
        List.of(large),
        config,
        262144
    );

    assertEquals(1, tasks.size());
    assertEquals("java-general", tasks.get(0).agentName());
    assertEquals(List.of(large), tasks.get(0).files());
  }

  @Test
  public void planReturnsEmptyWhenNoReviewableDiffs() {
    final Rule javaRule = new Rule("java-general", null, List.of("**/*.java"), "Java rules");

    final List<RulesetReviewTask> tasks = RulesetReviewPlanner.plan(
        List.of(javaRule),
        Map.of("src/App.java", List.of(javaRule)),
        List.of(ChangedFile.skipped("src/App.java", ChangeType.MODIFIED, "too large"))
    );

    assertTrue(tasks.isEmpty());
  }
}