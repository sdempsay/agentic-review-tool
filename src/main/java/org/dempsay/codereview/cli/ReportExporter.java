package org.dempsay.codereview.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalListener;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/**
 * Writes report Markdown to a file path.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class ReportExporter {

  private ReportExporter() {
  }

  /**
   * Writes Markdown to the output path.
   * 
   * @param outputPath the outputPath
   * @param markdown the markdown
   * @return the result
   * @since 1.0.0
 */
  public static ExceptionalResponse<Path> write(final Path outputPath, final String markdown) {
    return write(outputPath, markdown, null);
  }

  /**
   * Writes Markdown to the output path.
   * 
   * @param outputPath the outputPath
   * @param markdown the markdown
   * @param listener the listener
   * @return the result
   * @since 1.0.0
 */
  public static ExceptionalResponse<Path> write(
      final Path outputPath,
      final String markdown,
      final ExceptionalListener listener
  ) {
    if (outputPath == null) {
      return ExceptionalSupport.fail(listener, new IllegalArgumentException("output path is required"));
    }
    return ExceptionalSupport.supply(() -> {
      final Path parent = outputPath.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(outputPath, markdown);
      return outputPath.toAbsolutePath();
    }, listener);
  }
}
