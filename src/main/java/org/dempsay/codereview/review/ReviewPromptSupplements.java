package org.dempsay.codereview.review;

import java.nio.file.Path;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/** Shared prompt fragments loaded from the rules directory (guardrails, output format).
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record ReviewPromptSupplements(String guardrails, String outputFormat) {

  /**
   * Returns empty prompt supplements.
   * 
   * @return the result
   * @since 1.0.0
 */
  public static ReviewPromptSupplements empty() {
    return new ReviewPromptSupplements("", "");
  }

  /**
   * Loads configuration or resources.
   * 
   * @param rulesDir the rulesDir
   * @return the result
   * @since 1.0.0
 */
  public static ExceptionalResponse<ReviewPromptSupplements> load(final Path rulesDir) {
    return load(rulesDir, null);
  }

  /**
   * Loads configuration or resources.
   * 
   * @param rulesDir the rulesDir
   * @param listener the listener
   * @return the result
   * @since 1.0.0
 */
  public static ExceptionalResponse<ReviewPromptSupplements> load(
      final Path rulesDir,
      final ExceptionalListener listener
  ) {
    return ReviewGuardrailsLoader.load(rulesDir, listener)
        .chain((loadListener, guardrails) -> ReviewOutputFormatLoader.load(rulesDir, loadListener)
            .chain((formatListener, outputFormat) ->
                ExceptionalResponse.success(new ReviewPromptSupplements(guardrails, outputFormat)),
                loadListener),
            listener);
  }
}
