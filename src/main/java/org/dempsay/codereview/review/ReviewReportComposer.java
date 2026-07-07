package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.config.ModelConfig;
import org.dempsay.codereview.model.LlmTokenLedger;

public final class ReviewReportComposer {

  private ReviewReportComposer() {
  }

  public static String compose(final List<ReviewResult> agentResults, final String summary) {
    return compose(agentResults, summary, null, null);
  }

  public static String compose(
      final List<ReviewResult> agentResults,
      final String summary,
      final LlmTokenLedger tokenLedger,
      final ModelConfig model
  ) {
    final StringBuilder report = new StringBuilder();
    report.append("--- Agent Reviews ---");
    report.append(System.lineSeparator());
    report.append(ReviewAggregator.aggregate(agentResults));
    report.append(System.lineSeparator()).append(System.lineSeparator());
    report.append("--- Summary ---");
    report.append(System.lineSeparator());
    report.append(summary.trim());
    if (tokenLedger != null && model != null && !tokenLedger.isEmpty()) {
      report.append(System.lineSeparator()).append(System.lineSeparator());
      report.append(tokenLedger.formatReportSection(model));
    }
    return report.toString();
  }
}