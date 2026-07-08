package org.dempsay.codereview.ingest;

/**
 * Git diff scope for ingest requests.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public enum DiffScope {
  UNCOMMITTED,
  STAGED,
  BASE
}