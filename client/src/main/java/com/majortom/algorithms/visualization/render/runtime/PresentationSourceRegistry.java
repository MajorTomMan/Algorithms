package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.PresentationModelSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Scheduler-thread registry for JavaFX-neutral presentation model sources. */
final class PresentationSourceRegistry {
  private final Map<RenderSessionId, Entry<?>> entries = new HashMap<>();

  <M> void register(
      RenderSessionId id,
      Optional<RenderSessionId> cursorSessionId,
      PresentationModelSource<M> source) {
    entries.put(
        Objects.requireNonNull(id, "id"),
        new Entry<>(
            Objects.requireNonNull(cursorSessionId, "cursorSessionId"),
            Objects.requireNonNull(source, "source")));
  }

  void unregister(RenderSessionId id) {
    entries.remove(Objects.requireNonNull(id, "id"));
  }

  @SuppressWarnings("unchecked")
  <M> Entry<M> require(RenderSessionId id) {
    Entry<?> entry = entries.get(id);
    if (entry == null) throw new IllegalStateException("No presentation source registered for " + id);
    return (Entry<M>) entry;
  }

  List<RenderSessionId> dependentSurfaces(RenderSessionId cursorSessionId) {
    List<RenderSessionId> result = new ArrayList<>();
    for (Map.Entry<RenderSessionId, Entry<?>> entry : entries.entrySet()) {
      if (entry.getValue().cursorSessionId().filter(cursorSessionId::equals).isPresent()) {
        result.add(entry.getKey());
      }
    }
    return List.copyOf(result);
  }

  record Entry<M>(
      Optional<RenderSessionId> cursorSessionId, PresentationModelSource<M> source) {}
}
