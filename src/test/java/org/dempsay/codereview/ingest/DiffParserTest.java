package org.dempsay.codereview.ingest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiffParserTest {

  @Test
  public void parseModifiedFileDiff() {
    final var entries = DiffParser.parse(
        """
        diff --git a/src/App.java b/src/App.java
        index 1111111..2222222 100644
        --- a/src/App.java
        +++ b/src/App.java
        @@ -1 +1 @@
        -old
        +new
        """
    );

    assertEquals(1, entries.size());
    assertEquals("src/App.java", entries.get(0).path());
    assertEquals(ChangeType.MODIFIED, entries.get(0).changeType());
    assertFalse(entries.get(0).binary());
    assertTrue(entries.get(0).diff().contains("+new"));
  }

  @Test
  public void parseAddedAndDeletedFiles() {
    final var entries = DiffParser.parse(
        """
        diff --git a/removed.txt b/removed.txt
        deleted file mode 100644
        --- a/removed.txt
        +++ /dev/null
        @@ -1 +0,0 @@
        -gone

        diff --git a/new.txt b/new.txt
        new file mode 100644
        --- /dev/null
        +++ b/new.txt
        @@ -0,0 +1 @@
        +hello
        """
    );

    assertEquals(2, entries.size());
    assertEquals(ChangeType.DELETED, entries.get(0).changeType());
    assertEquals(ChangeType.ADDED, entries.get(1).changeType());
  }

  @Test
  public void detectBinaryDiff() {
    final var entries = DiffParser.parse(
        """
        diff --git a/image.png b/image.png
        Binary files a/image.png and b/image.png differ
        """
    );

    assertEquals(1, entries.size());
    assertTrue(entries.get(0).binary());
  }
}