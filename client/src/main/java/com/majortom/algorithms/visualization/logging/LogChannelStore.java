package com.majortom.algorithms.visualization.logging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Session-local repository of independent structure and algorithm logs. */
public final class LogChannelStore {
  private final Map<LogChannelId, LogChannel> channels = new LinkedHashMap<>();

  public LogChannel channel(LogChannelId id) {
    Objects.requireNonNull(id, "id");
    return channels.computeIfAbsent(id, LogChannel::new);
  }
}
