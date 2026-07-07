package org.dempsay.codereview.ingest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public class RepoPathFilterTest {

  @Test
  public void excludesMarkdownAndJsonByDefault() {
    final RepoIngestRequest request = RepoIngestRequest.of(Path.of("."), 512);

    assertTrue(RepoPathFilter.exclusionReason("README.md", request).isPresent());
    assertTrue(RepoPathFilter.exclusionReason("config.json", request).isPresent());
    assertFalse(RepoPathFilter.exclusionReason("src/App.java", request).isPresent());
  }

  @Test
  public void includeExtOverridesDefaultExclusions() {
    final RepoIngestRequest request = new RepoIngestRequest(
        Path.of("."),
        512,
        List.of(),
        List.of(".md"),
        List.of(),
        List.of()
    );

    assertFalse(RepoPathFilter.exclusionReason("README.md", request).isPresent());
    assertTrue(RepoPathFilter.exclusionReason("config.json", request).isPresent());
  }

  @Test
  public void pathGlobLimitsScope() {
    final RepoIngestRequest request = new RepoIngestRequest(
        Path.of("."),
        512,
        List.of("src/**"),
        List.of(),
        List.of(),
        List.of()
    );

    assertFalse(RepoPathFilter.exclusionReason("src/App.java", request).isPresent());
    assertTrue(RepoPathFilter.exclusionReason("docs/App.java", request).isPresent());
  }

  @Test
  public void excludeExtAddsToDefaultDenyList() {
    final RepoIngestRequest request = new RepoIngestRequest(
        Path.of("."),
        512,
        List.of(),
        List.of(),
        List.of(),
        List.of(".xml")
    );

    assertTrue(RepoPathFilter.exclusionReason("pom.xml", request).isPresent());
    assertTrue(RepoPathFilter.exclusionReason("README.md", request).isPresent());
  }

  @Test
  public void configExcludeExtensionsExtendDefaultDenyList() {
    final RepoIngestRequest request = new RepoIngestRequest(
        Path.of("."),
        512,
        List.of(),
        List.of(),
        List.of(".xml", ".yaml"),
        List.of()
    );

    assertTrue(RepoPathFilter.exclusionReason("pom.xml", request).isPresent());
    assertTrue(RepoPathFilter.exclusionReason("config.yaml", request).isPresent());
    assertTrue(RepoPathFilter.exclusionReason("README.md", request).isPresent());
    assertFalse(RepoPathFilter.exclusionReason("src/App.java", request).isPresent());
  }
}