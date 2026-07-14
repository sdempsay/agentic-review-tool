package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.dempsay.codereview.ingest.ChangeType;
import org.dempsay.codereview.ingest.ChangedFile;
import org.junit.Test;

public class DiffLineIndexTest {

  @Test
  public void indexesAddedLinesAndResolvesLineText() {
    final String diff = """
        diff --git a/src/App.java b/src/App.java
        --- a/src/App.java
        +++ b/src/App.java
        @@ -10,4 +10,5 @@
         public class App {
        -    if(foo){
        +    if (foo) {
             return;
         }
        """;

    final DiffLineIndex index = DiffLineIndex.fromFiles(
        List.of(ChangedFile.included("src/App.java", ChangeType.MODIFIED, diff))
    );

    assertTrue(index.hasAddedLine("src/App.java", 11));
    assertFalse(index.hasAddedLine("src/App.java", 10));
    assertEquals("    if (foo) {", index.lineText("src/App.java", 11));
  }
}