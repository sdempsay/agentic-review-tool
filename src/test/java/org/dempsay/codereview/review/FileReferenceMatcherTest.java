package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class FileReferenceMatcherTest {

  @Test
  public void findReferencedFileMatchesLongestFullPath() {
    final ChangedFile nested = ChangedFile.included("src/main/java/App.java", ChangeType.MODIFIED, "+a");
    final ChangedFile shortPath = ChangedFile.included("src/App.java", ChangeType.MODIFIED, "+b");

    final Optional<ChangedFile> match = FileReferenceMatcher.findReferencedFile(
        "Please inspect src/main/java/App.java indentation",
        List.of(shortPath, nested)
    );

    assertTrue(match.isPresent());
    assertEquals("src/main/java/App.java", match.get().path());
  }

  @Test
  public void findReferencedFileMatchesUniqueBasename() {
    final ChangedFile file = ChangedFile.included("src/main/java/AetherBuilderProcessor.java", ChangeType.MODIFIED, "+a");

    final Optional<ChangedFile> match = FileReferenceMatcher.findReferencedFile(
        "Look closer at AetherBuilderProcessor.java",
        List.of(file)
    );

    assertTrue(match.isPresent());
    assertEquals("src/main/java/AetherBuilderProcessor.java", match.get().path());
  }

  @Test
  public void findReferencedFileReturnsEmptyForAmbiguousBasename() {
    final Optional<ChangedFile> match = FileReferenceMatcher.findReferencedFile(
        "Compare Foo.java changes",
        List.of(
            ChangedFile.included("src/a/Foo.java", ChangeType.MODIFIED, "+a"),
            ChangedFile.included("src/b/Foo.java", ChangeType.MODIFIED, "+b")
        )
    );

    assertTrue(match.isEmpty());
  }
}