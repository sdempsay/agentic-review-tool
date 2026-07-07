package org.dempsay.codereview.support;

import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/**
 * Captures the first exceptional failure message for display at the CLI boundary.
 * See exceptional/WhyBeExceptional.md — failures stay explicit until the command edge.
 */
public final class FailureCapture {

  private String message;

  public ExceptionalListener listener() {
    return error -> message = error.getMessage();
  }

  public void failIfError(final ExceptionalResponse<?> response) {
    if (response.wasError()) {
      throw new CodeReviewException(message != null ? message : "Operation failed");
    }
  }
}