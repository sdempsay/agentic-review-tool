package org.dempsay.codereview.support;

import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/**
 * Captures the first exceptional failure message for display at the CLI boundary.
 * See exceptional/WhyBeExceptional.md — failures stay explicit until the command edge.
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class FailureCapture {

  private String message;

  /**
   * Returns an exceptional listener that captures the first error message.
   * 
   * @return the result
   * @since 1.0.0
 */
  public ExceptionalListener listener() {
    return error -> message = error.getMessage();
  }

  /**
   * Throws {@link CodeReviewException} when the response indicates failure.
   * 
   * @param response the response
   * @since 1.0.0
 */
  public void failIfError(final ExceptionalResponse<?> response) {
    if (response.wasError()) {
      throw new CodeReviewException(message != null ? message : "Operation failed");
    }
  }
}