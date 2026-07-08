package org.dempsay.codereview.review;

/**
 * Findings from a single agent review.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record ReviewResult(String agentName, String findings) {
}