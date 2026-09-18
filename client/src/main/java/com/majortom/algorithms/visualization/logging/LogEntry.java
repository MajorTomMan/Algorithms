package com.majortom.algorithms.visualization.logging;

import com.majortom.algorithms.core.logging.LogLevel;
import java.time.Instant;
import java.util.Objects;

/** Immutable entry retained by one structure/algorithm log channel. */
public record LogEntry(Instant timestamp, LogLevel level, String tag, String message) {
  public LogEntry {
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(level, "level");
    tag = tag == null ? "" : tag;
    message = message == null ? "" : message;
  }
}
