package org.dempsay.codereview.review;

import java.nio.file.Path;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/** Shared prompt fragments loaded from the rules directory (guardrails, output format). */
public record ReviewPromptSupplements(String guardrails, String outputFormat) {

  public static ExceptionalResponse<ReviewPromptSupplements> load(final Path rulesDir) {
    return ReviewGuardrailsLoader.load(rulesDir)
        .chain((listener, guardrails) -> ReviewOutputFormatLoader.load(rulesDir)
            .chain((formatListener, outputFormat) ->
                ExceptionalResponse.success(new ReviewPromptSupplements(guardrails, outputFormat)),
                listener));
  }
}