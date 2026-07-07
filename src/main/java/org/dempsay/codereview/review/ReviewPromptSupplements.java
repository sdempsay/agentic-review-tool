package org.dempsay.codereview.review;

import java.io.IOException;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/** Shared prompt fragments loaded from the rules directory (guardrails, output format). */
public record ReviewPromptSupplements(String guardrails, String outputFormat) {

  public static ExceptionalResponse<ReviewPromptSupplements> load(final Path rulesDir) {
    return ExceptionalSupport.supply(() -> loadRequired(rulesDir));
  }

  public static ReviewPromptSupplements loadRequired(final Path rulesDir) throws IOException {
    return new ReviewPromptSupplements(
        ReviewGuardrailsLoader.loadRequired(rulesDir),
        ReviewOutputFormatLoader.loadRequired(rulesDir)
    );
  }
}