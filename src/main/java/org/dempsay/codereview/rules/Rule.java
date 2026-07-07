package org.dempsay.codereview.rules;

import java.nio.file.Path;
import java.util.List;

public record Rule(String id, Path source, List<String> pathGlobs, String promptBody) {
}