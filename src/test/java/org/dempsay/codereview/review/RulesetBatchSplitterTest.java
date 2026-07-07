package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class RulesetBatchSplitterTest {

  @Test
  public void splitReturnsSingleBatchWhenUnderLimits() {
    final List<List<ChangedFile>> batches = RulesetBatchSplitter.split(
        List.of(
            ChangedFile.included("a.java", ChangeType.MODIFIED, "a".repeat(100)),
            ChangedFile.included("b.java", ChangeType.MODIFIED, "b".repeat(100))
        ),
        256,
        0
    );

    assertEquals(1, batches.size());
    assertEquals(2, batches.get(0).size());
  }

  @Test
  public void splitByMaxAgentDiffKb() {
    final List<List<ChangedFile>> batches = RulesetBatchSplitter.split(
        List.of(
            ChangedFile.included("a.java", ChangeType.MODIFIED, "a".repeat(600)),
            ChangedFile.included("b.java", ChangeType.MODIFIED, "b".repeat(600))
        ),
        1,
        0
    );

    assertEquals(2, batches.size());
    assertEquals("a.java", batches.get(0).get(0).path());
    assertEquals("b.java", batches.get(1).get(0).path());
  }

  @Test
  public void splitByMaxFilesPerAgent() {
    final List<List<ChangedFile>> batches = RulesetBatchSplitter.split(
        List.of(
            ChangedFile.included("a.java", ChangeType.MODIFIED, "+a"),
            ChangedFile.included("b.java", ChangeType.MODIFIED, "+b"),
            ChangedFile.included("c.java", ChangeType.MODIFIED, "+c")
        ),
        0,
        2
    );

    assertEquals(2, batches.size());
    assertEquals(2, batches.get(0).size());
    assertEquals(1, batches.get(1).size());
  }
}