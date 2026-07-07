package org.dempsay.codereview.cli;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class RepoScopeDescriberTest {

  @Test
  public void describeUsesBaseScopeWhenNoFilters() {
    assertEquals(
        "repository (tracked + untracked)",
        RepoScopeDescriber.describe(List.of(), List.of(), List.of())
    );
  }

  @Test
  public void describeIncludesPathIncludeAndExcludeFlags() {
    assertEquals(
        "repository (tracked + untracked); --path src/**; --include-ext .java; --exclude-ext .xml",
        RepoScopeDescriber.describe(List.of("src/**"), List.of(".java"), List.of(".xml"))
    );
  }
}