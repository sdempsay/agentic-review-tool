package org.dempsay.codereview.rules;

import java.nio.file.Path;
import java.util.List;

/**
 * A review ruleset with path globs and prompt body.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public record Rule(String id, Path source, List<String> pathGlobs, String promptBody) {
}