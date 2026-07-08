package org.dempsay.codereview.review;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AgentFindingsSanitizerTest {

  @Test
  public void forSummarizeCollapsesRetractedFindingsEndingClean() {
    final String findings = """
        - `App.java:10` — nit — brace style
        **Note**: these findings are **invalid** under diff discipline.

        Re-evaluating strictly on `+` lines:

        - `App.java:12` — nit — spacing

        ## Clean
        """;

    assertEquals("## Clean", AgentFindingsSanitizer.forSummarize(findings));
  }

  @Test
  public void forSummarizeKeepsFindingsWhenAgentStoodByThem() {
    final String findings = """
        - `App.java:10` — must-fix — missing listener
        Clean: all other files in scope
        """;

    assertEquals(findings.trim(), AgentFindingsSanitizer.forSummarize(findings));
  }

  @Test
  public void forSummarizeKeepsPlainCleanVerdict() {
    assertEquals("## Clean", AgentFindingsSanitizer.forSummarize("## Clean"));
  }

  @Test
  public void forSummarizePassesThroughReevaluationWithoutRetractionMarkers() {
    final String findings = """
        - `App.java:10` — nit — line length
        ## Clean
        """;

    assertEquals(findings.trim(), AgentFindingsSanitizer.forSummarize(findings));
  }
}