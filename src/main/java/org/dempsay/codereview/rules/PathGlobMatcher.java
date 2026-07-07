package org.dempsay.codereview.rules;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

public final class PathGlobMatcher {

  private PathGlobMatcher() {
  }

  public static boolean matches(final String glob, final String filePath) {
    final String normalizedPath = filePath.replace('\\', '/');
    final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
    return matcher.matches(Path.of(normalizedPath));
  }
}