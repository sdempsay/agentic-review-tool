package org.dempsay.codereview.support;

import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.dempsay.utils.exceptional.api.ExceptionalSupplier;
import org.dempsay.utils.exceptional.api.ExceptionalSupplierCall;

/** Test helpers and thin wrappers around {@link ExceptionalSupplier}. */
public final class ExceptionalSupport {

  private ExceptionalSupport() {
  }

  public static <T> ExceptionalResponse<T> supply(final ExceptionalSupplierCall<T> call) {
    return supply(call, null);
  }

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

  public static <T> T response(final ExceptionalResponse<T> exceptionalResponse) {
    if (exceptionalResponse.wasError()) {
      throw new AssertionError("Expected successful exceptional response");
    }
    return exceptionalResponse.response();
  }
}