package org.dempsay.codereview.ingest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.junit.Test;

public class DiffIngestServiceTest {

  @Test
  public void ingestTextParsesUnifiedDiffWithoutGit() {
    final var files = ExceptionalSupport.response(DiffIngestService.ingestText(
        """
        diff --git a/src/App.java b/src/App.java
        index 1111111..2222222 100644
        --- a/src/App.java
        +++ b/src/App.java
        @@ -1 +1 @@
        -old
        +new
        """,
        512
    ));

    assertEquals(1, files.size());
    assertEquals("src/App.java", files.get(0).path());
    assertTrue(files.get(0).hasDiff());
    assertTrue(files.get(0).diff().contains("+new"));
  }

  @Test
  public void ingestExternalReadsDiffFile() throws Exception {
    final Path diffFile = Files.createTempFile("code-review-external-diff", ".patch");
    Files.writeString(
        diffFile,
        """
        diff --git a/Foo.java b/Foo.java
        --- a/Foo.java
        +++ b/Foo.java
        @@ -1 +1 @@
        -a
        +b
        """
    );

    final var files = ExceptionalSupport.response(
        DiffIngestService.ingestExternal(false, java.util.List.of(diffFile), 512, null)
    );

    assertEquals(1, files.size());
    assertEquals("Foo.java", files.get(0).path());
    assertTrue(files.get(0).diff().contains("+b"));
  }
}