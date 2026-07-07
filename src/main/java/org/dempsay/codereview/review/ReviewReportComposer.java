package org.dempsay.codereview.review;

import java.util.List;

public final class ReviewReportComposer {

  private ReviewReportComposer() {
  }

  public static String compose(final List<ReviewResult> agentResults, final String summary) {
    final StringBuilder report = new StringBuilder();
    report.append("--- Agent Reviews ---");
    report.append(System.lineSeparator());
    report.append(ReviewAggregator.aggregate(agentResults));
    report.append(System.lineSeparator()).append(System.lineSeparator());
    report.append("--- Summary ---");
    report.append(System.lineSeparator());
    report.append(summary.trim());
    return report.toString();
  }
}