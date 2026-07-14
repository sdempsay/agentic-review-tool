package org.dempsay.codereview.review;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class ReviewOutputValidatorExceptionalScanTest {

  @Test
  public void validateRejectsFalseCleanWhenDiffScanFindsThrows() {
    final String path = "widget-store/src/main/java/com/example/store/fs/DocumentIo.java";
    final String diff = """
        diff --git a/widget-store/src/main/java/com/example/store/fs/DocumentIo.java b/widget-store/src/main/java/com/example/store/fs/DocumentIo.java
        --- /dev/null
        +++ b/widget-store/src/main/java/com/example/store/fs/DocumentIo.java
        @@ -0,0 +1,2 @@
        +    Payload read(final Path path) throws IOException {
        """.stripIndent();

    final var result = ReviewOutputValidator.validate(
        "java-exceptional",
        List.of(ChangedFile.included(path, ChangeType.ADDED, diff)),
        "## Clean"
    );

    assertFalse(result.valid());
    assertTrue(result.violations().stream().anyMatch(v -> v.contains("diff scan")));
  }
}