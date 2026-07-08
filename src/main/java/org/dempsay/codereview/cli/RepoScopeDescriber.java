package org.dempsay.codereview.cli;

import java.util.List;

/**
 * Describes repo review scope for reports.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class RepoScopeDescriber {

  private RepoScopeDescriber() {
  }

  /**
   * Describes the repo review scope.
   * 
   * @param pathGlobs the pathGlobs
   * @param includeExtensions the includeExtensions
   * @param excludeExtensions the excludeExtensions
   * @return the result
   * @since 1.0.0
 */
  public static String describe(
      final List<String> pathGlobs,
      final List<String> includeExtensions,
      final List<String> excludeExtensions
  ) {
    final StringBuilder scope = new StringBuilder("repository (tracked + untracked)");
    appendFlagList(scope, "--path", pathGlobs);
    appendFlagList(scope, "--include-ext", includeExtensions);
    appendFlagList(scope, "--exclude-ext", excludeExtensions);
    return scope.toString();
  }

  private static void appendFlagList(final StringBuilder scope, final String flag, final List<String> values) {
    if (values == null || values.isEmpty()) {
      return;
    }
    scope.append("; ").append(flag).append(' ').append(String.join(", ", values));
  }
}