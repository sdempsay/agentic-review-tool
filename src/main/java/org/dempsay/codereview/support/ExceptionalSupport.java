package org.dempsay.codereview.support;

import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.dempsay.utils.exceptional.api.ExceptionalSupplier;
import org.dempsay.utils.exceptional.api.ExceptionalSupplierCall;

/**
 * Thin wrappers around {@link ExceptionalSupplier}.
 *
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class ExceptionalSupport {

  private ExceptionalSupport() {
  }

  /**
   * Executes a supplier and returns an exceptional response.
   * 
   * @param call the call
   * @return the result
   * @since 1.0.0
 */
  public static <T> ExceptionalResponse<T> supply(final ExceptionalSupplierCall<T> call) {
    return supply(call, null);
  }

  /**
   * Executes a supplier and returns an exceptional response.
   * 
   * @param call the call
   * @param listener the listener
   * @return the result
   * @since 1.0.0
 */
  public static <T> ExceptionalResponse<T> supply(
      final ExceptionalSupplierCall<T> call,
      final ExceptionalListener listener
  ) {
    final ExceptionalSupplier<T> supplier = ExceptionalSupplier.of(call);
    if (listener != null) {
      supplier.with(listener);
    }
    return supplier.execute();
  }

  /**
   * Records a failure without {@code throw} in application chain/supplier lambdas.
   * Prefer this over {@code throw} inside {@code .chain()} callbacks.
   * @since 1.0.0
 */
  public static <T> ExceptionalResponse<T> fail(final ExceptionalListener listener, final Exception error) {
    if (listener != null) {
      listener.onError(error);
    }
    return ExceptionalResponse.failure();
  }

  /**
   * Top-level failure when no listener is in scope. Uses exceptional capture internally.
   * @since 1.0.0
 */
  public static <T> ExceptionalResponse<T> fail(final Exception error) {
    return supply(() -> {
      if (error instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw new IllegalStateException(error);
    });
  }

  /** Unwraps a successful response in tests only.
   * @since 1.0.0
 */
  public static <T> T response(final ExceptionalResponse<T> exceptionalResponse) {
    if (exceptionalResponse.wasError()) {
      throw new AssertionError("Expected successful exceptional response");
    }
    return exceptionalResponse.response();
  }
}