package org.dempsay.codereview.review;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.dempsay.codereview.support.ExceptionalSupport;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/**
 * Loads shared review-agent output instructions from {@code review-output-format.md}.
 * Resolves {@code rulesDir/review-output-format.md} first, then the bundled classpath copy.
 */
public final class ReviewOutputFormatLoader {

  public static final String FILE_NAME = "review-output-format.md";
  private static final String BUNDLED_RESOURCE = "/rules/review-output-format.md";

  private ReviewOutputFormatLoader() {
  }

  public static ExceptionalResponse<String> load(final Path rulesDir) {
    return ExceptionalSupport.supply(() -> loadRequired(rulesDir));
  }

  public static String loadRequired(final Path rulesDir) throws IOException {
    if (rulesDir != null) {
      final Path file = rulesDir.resolve(FILE_NAME);
      if (Files.isRegularFile(file)) {
        return Files.readString(file);
      }
    }
    try (InputStream input = ReviewOutputFormatLoader.class.getResourceAsStream(BUNDLED_RESOURCE)) {
      if (input == null) {
        throw new IOException("Missing bundled resource: " + BUNDLED_RESOURCE);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}