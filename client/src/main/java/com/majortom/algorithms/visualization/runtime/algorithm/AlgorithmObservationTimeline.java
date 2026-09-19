package com.majortom.algorithms.visualization.runtime.algorithm;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Prefix snapshots for exact event-index seek; UI refresh does not replay from event zero each time. */
public final class AlgorithmObservationTimeline {
  private final List<EventEnvelope> events = new ArrayList<>();
  private final List<AlgorithmObservationModel> states = new ArrayList<>();

  public synchronized AlgorithmObservationModel at(List<EventEnvelope> authoritativeEvents, int index) {
    Objects.requireNonNull(authoritativeEvents, "authoritativeEvents");
    if (index < 0 || authoritativeEvents.isEmpty()) return AlgorithmObservationModel.empty();
    int last = Math.min(index, authoritativeEvents.size() - 1);
    int common = 0;
    while (common < events.size() && common <= last
        && events.get(common) == authoritativeEvents.get(common)) common++;
    if (common < events.size()) {
      events.subList(common, events.size()).clear();
      states.subList(common, states.size()).clear();
    }
    AlgorithmObservationModel state = common == 0 ? AlgorithmObservationModel.empty() : states.get(common - 1);
    for (int position = common; position <= last; position++) {
      EventEnvelope envelope = authoritativeEvents.get(position);
      state = AlgorithmObservationModel.apply(state, envelope);
      events.add(envelope);
      states.add(state);
    }
    return states.get(last);
  }

  public synchronized void clear() {
    events.clear();
    states.clear();
  }
}
