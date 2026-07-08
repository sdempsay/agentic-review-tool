package org.dempsay.codereview.rules;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Matches file paths against glob patterns.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class PathGlobMatcher {

  private PathGlobMatcher() {
  }

  /**
   * Returns whether a file path matches a glob pattern.
   * 
   * @param glob the glob
   * @param filePath the filePath
   * @return the result
   * @since 1.0.0
 */
  public static boolean matches(final String glob, final String filePath) {
    final String normalizedPath = filePath.replace('\\', '/');
    final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
    return matcher.matches(Path.of(normalizedPath));
  }
}