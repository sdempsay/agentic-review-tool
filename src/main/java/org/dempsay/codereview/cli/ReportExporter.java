package org.dempsay.codereview.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

public final class ReportExporter {

  private ReportExporter() {
  }

  public static ExceptionalResponse<Path> write(final Path outputPath, final String markdown) {
    return write(outputPath, markdown, null);
  }

  public static ExceptionalResponse<Path> write(
      final Path outputPath,
      final String markdown,
      final ExceptionalListener listener
  ) {
    return ExceptionalSupport.supply(() -> {
      if (outputPath == null) {
        throw new IllegalArgumentException("output path is required");
      }
      final Path parent = outputPath.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(outputPath, markdown);
      return outputPath.toAbsolutePath();
    }, listener);
  }
}