package org.dempsay.codereview.config;

import java.nio.file.Path;
import java.util.List;

public record AppConfig(
    ModelConfig model,
    Path rulesDir,
    int maxTokens,
    int maxDiffKb,
    int maxAgentDiffKb,
    int maxFilesPerAgent,
    List<String> repoExcludeExtensions
) {
  public AppConfig {
    repoExcludeExtensions = List.copyOf(repoExcludeExtensions == null ? List.of() : repoExcludeExtensions);
  }
}