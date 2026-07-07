package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class ReviewContentModeTest {

  @Test
  public void resolveReturnsFullFileWhenAllExisting() {
    assertEquals(
        ReviewContentMode.FULL_FILE,
        ReviewContentMode.resolve(List.of(ChangedFile.included("src/App.java", ChangeType.EXISTING, "code")))
    );
  }

  @Test
  public void resolveReturnsDiffForModifiedFiles() {
    assertEquals(
        ReviewContentMode.DIFF,
        ReviewContentMode.resolve(List.of(ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+line")))
    );
  }

  @Test(expected = IllegalArgumentException.class)
  public void resolveRejectsMixedModes() {
    ReviewContentMode.resolve(List.of(
        ChangedFile.included("src/App.java", ChangeType.EXISTING, "code"),
        ChangedFile.included("src/Other.java", ChangeType.MODIFIED, "+line")
    ));
  }
}