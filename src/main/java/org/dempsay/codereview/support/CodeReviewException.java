package org.dempsay.codereview.support;

/**
 * Runtime exception for CLI boundary failures.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public class CodeReviewException extends RuntimeException {

  /**
   * Creates a new CodeReviewException.
   * 
   * @param message the message
   * @since 1.0.0
 */
  public CodeReviewException(final String message) {
    super(message);
  }
}