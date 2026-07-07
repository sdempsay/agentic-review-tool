package org.dempsay.codereview.review;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dempsay.codereview.ingest.ChangedFile;

public final class RepoHotspotAnalyzer {

  private static final int DEFAULT_DEPTH = 2;
  private static final int DEFAULT_LIMIT = 8;

  private RepoHotspotAnalyzer() {
  }

  public static List<HotspotArea> topAreas(final List<ChangedFile> changedFiles) {
    return topAreas(changedFiles, DEFAULT_DEPTH, DEFAULT_LIMIT);
  }

  public static List<HotspotArea> topAreas(
      final List<ChangedFile> changedFiles,
      final int pathDepth,
      final int limit
  ) {
    final Map<String, Integer> counts = new LinkedHashMap<>();
    for (final ChangedFile file : changedFiles) {
      if (!file.hasDiff()) {
        continue;
      }
      final String area = areaKey(file.path(), pathDepth);
      counts.merge(area, 1, Integer::sum);
    }

    return counts.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
            .thenComparing(Map.Entry.comparingByKey()))
        .limit(limit)
        .map(entry -> new HotspotArea(entry.getKey(), entry.getValue()))
        .toList();
  }

  static String areaKey(final String path, final int pathDepth) {
    final String normalized = path.replace('\\', '/');
    final int slash = normalized.lastIndexOf('/');
    if (slash < 0) {
      return "(root)";
    }

    final String directory = normalized.substring(0, slash);
    final String[] segments = directory.split("/");
    if (segments.length <= pathDepth) {
      return directory;
    }

    final List<String> prefix = new ArrayList<>(pathDepth);
    for (int index = 0; index < pathDepth; index++) {
      prefix.add(segments[index]);
    }
    return String.join("/", prefix);
  }

  public record HotspotArea(String path, int reviewableFiles) {
  }
}