package org.dempsay.codereview.config;

import java.nio.file.Path;

public record AppConfig(ModelConfig model, Path rulesDir, int maxTokens, int maxDiffKb) {
}