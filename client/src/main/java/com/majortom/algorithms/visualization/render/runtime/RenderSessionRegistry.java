package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.LinkedHashMap;
import java.util.Map;

final class RenderSessionRegistry {
  private final Map<RenderSessionId, RenderSession> sessions = new LinkedHashMap<>();
  RenderSession getOrCreate(RenderSessionId id) {
    return sessions.computeIfAbsent(id, RenderSession::new);
  }
  RenderSession get(RenderSessionId id) {
    return sessions.get(id);
  }
  java.util.Collection<RenderSession> values() {
    return java.util.List.copyOf(sessions.values());
  }
}
