package org.dempsay.codereview.review;

import java.util.List;
import java.util.Map;
import org.dempsay.codereview.config.AppConfig;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.rules.Rule;

/**
 * Context for follow-up chat after a review.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record ReviewSessionContext(
    AppConfig config,
    List<Rule> rules,
    List<ChangedFile> changedFiles,
    Map<String, List<Rule>> classification,
    String reportText
) {
}