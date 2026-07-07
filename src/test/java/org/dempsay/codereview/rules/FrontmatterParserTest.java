package org.dempsay.codereview.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.dempsay.codereview.support.ExceptionalSupport;
import org.junit.Test;

public class FrontmatterParserTest {

  @Test
  public void parseValidFrontmatter() {
    final FrontmatterParser.ParsedRuleDocument document = ExceptionalSupport.response(
        FrontmatterParser.parse(
            """
            ---
            paths:
              - "**/*.java"
              - "**/service/**"
            ---

            # Prompt body
            Review this code.
            """
        )
    );

    assertEquals(2, document.pathGlobs().size());
    assertEquals("**/*.java", document.pathGlobs().get(0));
    assertTrue(document.promptBody().contains("Review this code."));
  }

  @Test
  public void rejectMissingFrontmatterDelimiter() {
    assertTrue(FrontmatterParser.parse("# No frontmatter").wasError());
  }

  @Test
  public void rejectMissingPathsField() {
    assertTrue(
        FrontmatterParser.parse(
            """
            ---
            title: broken
            ---

            Body
            """
        ).wasError()
    );
  }

  @Test
  public void rejectEmptyPathsList() {
    assertTrue(
        FrontmatterParser.parse(
            """
            ---
            paths: []
            ---

            Body
            """
        ).wasError()
    );
  }
}