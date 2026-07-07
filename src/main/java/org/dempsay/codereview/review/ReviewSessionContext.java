package org.dempsay.codereview.review;

import java.util.List;
import java.util.Map;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

public record ReviewSessionContext(
    AppConfig config,
    List<Rule> rules,
    List<ChangedFile> changedFiles,
    Map<String, List<Rule>> classification,
    String reportText
) {
}