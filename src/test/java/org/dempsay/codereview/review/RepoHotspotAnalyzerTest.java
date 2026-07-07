package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class RepoHotspotAnalyzerTest {

  @Test
  public void topAreasGroupsByPathPrefixAndSortsByFileCount() {
    final List<ChangedFile> files = List.of(
        included("src/main/java/cli/RepoCommand.java"),
        included("src/main/java/cli/DiffCommand.java"),
        included("src/main/java/review/LlmReviewService.java"),
        included("src/test/java/cli/RepoCommandTest.java"),
        included("README.md")
    );

    final List<RepoHotspotAnalyzer.HotspotArea> areas = RepoHotspotAnalyzer.topAreas(files);

    assertEquals(3, areas.size());
    assertEquals("src/main", areas.get(0).path());
    assertEquals(3, areas.get(0).reviewableFiles());
    assertEquals("(root)", areas.get(1).path());
    assertEquals(1, areas.get(1).reviewableFiles());
    assertEquals("src/test", areas.get(2).path());
    assertEquals(1, areas.get(2).reviewableFiles());
  }

  @Test
  public void areaKeyUsesRootLabelForTopLevelFiles() {
    assertEquals("(root)", RepoHotspotAnalyzer.areaKey("App.java", 2));
    assertEquals("src/main", RepoHotspotAnalyzer.areaKey("src/main/java/App.java", 2));
    assertEquals("src/main/java", RepoHotspotAnalyzer.areaKey("src/main/java/org/App.java", 3));
  }

  private static ChangedFile included(final String path) {
    return ChangedFile.included(path, ChangeType.EXISTING, "content");
  }
}