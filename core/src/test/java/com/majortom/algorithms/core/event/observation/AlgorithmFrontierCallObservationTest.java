package com.majortom.algorithms.core.event.observation;

import static org.junit.jupiter.api.Assertions.*;

import com.majortom.algorithms.core.event.structure.StructureEvent;
import com.majortom.algorithms.core.runtime.*;
import com.majortom.algorithms.core.statistics.MetricKeys;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgorithmFrontierCallObservationTest {
  @Test
  void frontierAndRecursionUseExistingObservationStreamAndSeparateStatistics() {
    var sink = new InMemoryEventSink();
    var result = new ExecutionRuntime().execute("generic", sink, () -> {
      Observations.candidateAdded("frontier", "candidate-1", new ObservationEvent.IndexRef("array", 0));
      Observations.candidateSelected("frontier", "candidate-1");
      Observations.candidateRejected("frontier", "candidate-1");
      Observations.candidatePruned("frontier", "candidate-1");
      Observations.callEntered("root", null, "visit(root)");
      Observations.callEntered("child", "root", "visit(child)");
      Observations.callReturned("child", "ok");
      Observations.callReturned("root", "ok");
      return 123;
    });
    assertEquals(123, result.output().orElseThrow());
    List<EventEnvelope> events = sink.events();
    assertEquals(10, events.size());
    assertEquals(8, events.stream().filter(e -> e.event() instanceof AlgorithmObservationEvent).count());
    assertFalse(events.stream().anyMatch(e -> e.event() instanceof StructureEvent));
    for (int i = 0; i < events.size(); i++) assertEquals(i, events.get(i).sequence());
    var reducer = new StatisticsReducer();
    var stats = reducer.initialState();
    for (EventEnvelope envelope : events) stats = reducer.reduce(stats, envelope);
    assertEquals(1L, stats.metric(MetricKeys.FRONTIER_ADDITIONS));
    assertEquals(1L, stats.metric(MetricKeys.FRONTIER_SELECTIONS));
    assertEquals(1L, stats.metric(MetricKeys.FRONTIER_REJECTIONS));
    assertEquals(1L, stats.metric(MetricKeys.FRONTIER_PRUNES));
    assertEquals(2L, stats.metric(MetricKeys.RECURSIVE_CALLS));
    assertEquals(2L, stats.metric(MetricKeys.RECURSIVE_RETURNS));
    assertFalse(stats.metrics().containsKey(MetricKeys.NODES_VISITED));
  }

  @Test
  void invalidFactsAreRejectedAtConstruction() {
    assertThrows(IllegalArgumentException.class, () -> new AlgorithmObservationEvent.CandidateAdded(
        " ", "a", new ObservationEvent.ValueRef(1)));
    assertThrows(NullPointerException.class, () -> new AlgorithmObservationEvent.CandidateAdded(
        "f", "a", null));
    assertThrows(IllegalArgumentException.class, () -> new AlgorithmObservationEvent.CallEntered(
        "a", "a", "x"));
    assertThrows(IllegalArgumentException.class, () -> new AlgorithmObservationEvent.CallEntered(
        "a", null, " "));
  }
}
