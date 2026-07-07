package org.dempsay.codereview.review;

import java.util.List;

public final class ReviewAggregator {

  private ReviewAggregator() {
  }

  public static String aggregate(final List<ReviewResult> results) {
    if (results.isEmpty()) {
      return "No reviewable diffs found.";
    }

    final StringBuilder output = new StringBuilder();
    for (int index = 0; index < results.size(); index++) {
      if (index > 0) {
        output.append(System.lineSeparator()).append(System.lineSeparator());
      }
      final ReviewResult result = results.get(index);
      output.append("=== Review: ").append(result.agentName()).append(" ===");
      output.append(System.lineSeparator());
      output.append(result.findings().trim());
    }
    return output.toString();
  }
}