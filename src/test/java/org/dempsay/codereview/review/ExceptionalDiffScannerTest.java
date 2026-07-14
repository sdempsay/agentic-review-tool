package org.dempsay.codereview.review;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class ExceptionalDiffScannerTest {

  private static final String ROOT_MAIN_PATH = "src/main/java/com/example/store/fs/DocumentIo.java";
  private static final String MODULE_MAIN_PATH = "widget-store/src/main/java/com/example/store/fs/DocumentIo.java";

  @Test
  public void scanFindsThrowsOnMainJavaPlusLines() {
    final String diff = """
        diff --git a/src/main/java/com/example/store/fs/DocumentIo.java b/src/main/java/com/example/store/fs/DocumentIo.java
        --- a/src/main/java/com/example/store/fs/DocumentIo.java
        +++ b/src/main/java/com/example/store/fs/DocumentIo.java
        @@ -0,0 +1,3 @@
        +final class DocumentIo {
        +    Payload read(final Path path) throws IOException {
        +    }
        """.stripIndent();

    final List<String> violations = ExceptionalDiffScanner.scanMainJavaPlusLines(
        "java-exceptional",
        List.of(ChangedFile.included(ROOT_MAIN_PATH, ChangeType.ADDED, diff))
    );

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.contains("throws")));
  }

  @Test
  public void scanFindsThrowsInModulePrefixedMainJavaPaths() {
    final String diff = """
        diff --git a/widget-store/src/main/java/com/example/store/fs/DocumentIo.java b/widget-store/src/main/java/com/example/store/fs/DocumentIo.java
        --- /dev/null
        +++ b/widget-store/src/main/java/com/example/store/fs/DocumentIo.java
        @@ -0,0 +1,2 @@
        +    Payload read(final Path path) throws IOException {
        """.stripIndent();

    final List<String> violations = ExceptionalDiffScanner.scanMainJavaPlusLines(
        "java-exceptional",
        List.of(ChangedFile.included(MODULE_MAIN_PATH, ChangeType.ADDED, diff))
    );

    assertFalse(violations.isEmpty());
  }

  @Test
  public void scanSkipsTestSources() {
    final String diff = """
        diff --git a/src/test/java/DemoTest.java b/src/test/java/DemoTest.java
        --- a/src/test/java/DemoTest.java
        +++ b/src/test/java/DemoTest.java
        @@ -1,1 +1,2 @@
         class DemoTest {
        +    void go() throws Exception {
        """.stripIndent();

    final List<String> violations = ExceptionalDiffScanner.scanMainJavaPlusLines(
        "java-exceptional",
        List.of(ChangedFile.included("src/test/java/DemoTest.java", ChangeType.MODIFIED, diff))
    );

    assertTrue(violations.isEmpty());
  }
}