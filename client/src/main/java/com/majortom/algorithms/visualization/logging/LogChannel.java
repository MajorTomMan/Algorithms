package com.majortom.algorithms.visualization.logging;

import com.majortom.algorithms.core.logging.LogEvent;
import com.majortom.algorithms.core.logging.LogLevel;
import java.time.Instant;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** One independently retained log stream. */
public final class LogChannel {
  private final LogChannelId id;
  private final ObservableList<LogEntry> entries = FXCollections.observableArrayList();

  LogChannel(LogChannelId id) {
    this.id = Objects.requireNonNull(id, "id");
  }

  public LogChannelId id() {
    return id;
  }

  ObservableList<LogEntry> entries() {
    return entries;
  }

  public void append(LogEvent event, Instant timestamp) {
    Objects.requireNonNull(event, "event");
    append(timestamp, event.level(), event.tag(), event.message());
  }

  public void append(LogLevel level, String tag, String message) {
    append(Instant.now(), level, tag, message);
  }

  public void appendSystem(String message) {
    append(LogLevel.INFO, "SYSTEM", message);
  }

  public void append(Instant timestamp, LogLevel level, String tag, String message) {
    entries.add(new LogEntry(timestamp, level, tag, message));
  }
}
