package org.dempsay.codereview.review;

import java.util.List;
import org.dempsay.codereview.cli.ReviewProgress;
import org.dempsay.codereview.config.ModelConfig;
import org.dempsay.codereview.ingest.ChangedFile;

/**
 * Inputs for a guarded agent review LLM completion.
 *
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
record AgentReviewRequest(
    ModelConfig model,
    int maxTokens,
    String prompt,
    ReviewProgress progress,
    String label,
    String agentName,
    List<ChangedFile> scopedFiles
) {
}
