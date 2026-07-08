package org.dempsay.codereview.ingest;

/**
 * Git change classification for ingested files.
 * 
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public enum ChangeType {
  ADDED,
  MODIFIED,
  DELETED,
  RENAMED,
  EXISTING
}