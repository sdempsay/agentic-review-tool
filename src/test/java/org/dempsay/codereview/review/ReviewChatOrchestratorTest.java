package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;
import org.junit.Test;

public class ReviewChatOrchestratorTest {

  @Test
  public void resolveDelegationRoutesToMatchingRuleset() {
    final Rule javaRule = new Rule("java-general", Path.of("java-general.md"), List.of("**/*.java"), "rules");
    final ChangedFile file = ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+line");

    final Optional<DelegationTarget> delegation = ReviewChatOrchestrator.resolveDelegation(
        "Why was src/App.java flagged?",
        List.of(file),
        Map.of("src/App.java", List.of(javaRule))
    );

    assertTrue(delegation.isPresent());
    assertEquals("java-general", delegation.get().agentName());
    assertEquals("src/App.java", delegation.get().file().path());
  }

  @Test
  public void resolveDelegationUsesGeneralFallbackWhenNoRulesMatch() {
    final ChangedFile file = ChangedFile.included("README.md", ChangeType.MODIFIED, "+docs");

    final Optional<DelegationTarget> delegation = ReviewChatOrchestrator.resolveDelegation(
        "Is README.md okay?",
        List.of(file),
        Map.of("README.md", List.of())
    );

    assertTrue(delegation.isPresent());
    assertEquals("general", delegation.get().agentName());
  }

  @Test
  public void resolveDelegationReturnsEmptyWhenNoFileReferenced() {
    final Optional<DelegationTarget> delegation = ReviewChatOrchestrator.resolveDelegation(
        "Should I merge this?",
        List.of(ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+line")),
        Map.of()
    );

    assertTrue(delegation.isEmpty());
  }
}