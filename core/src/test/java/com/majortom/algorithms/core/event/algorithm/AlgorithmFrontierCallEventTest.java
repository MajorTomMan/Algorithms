package com.majortom.algorithms.core.event.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.majortom.algorithms.core.event.structure.StructureEvent;
import com.majortom.algorithms.core.runtime.*;
import com.majortom.algorithms.core.statistics.MetricKeys;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgorithmFrontierCallEventTest {
  @Test
  void frontierAndRecursionUseExistingObservationStreamAndSeparateStatistics() {
    var sink = new InMemoryEventSink();
    var result = new ExecutionRuntime().execute("generic", sink, () -> {
      AlgorithmEvents.candidateAdded("frontier", "candidate-1", new AlgorithmEvent.IndexRef("array", 0));
      AlgorithmEvents.candidateSelected("frontier", "candidate-1");
      AlgorithmEvents.candidateRejected("frontier", "candidate-1");
      AlgorithmEvents.candidatePruned("frontier", "candidate-1");
      AlgorithmEvents.callEntered("root", null, "visit(root)");
      AlgorithmEvents.callEntered("child", "root", "visit(child)");
      AlgorithmEvents.callReturned("child", "ok");
      AlgorithmEvents.callReturned("root", "ok");
      return 123;
    });
    assertEquals(123, result.output().orElseThrow());
    List<EventEnvelope> events = sink.events();
    assertEquals(10, events.size());
    assertEquals(8, events.stream().filter(e -> e.event() instanceof AlgorithmEvent).count());
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
    assertThrows(IllegalArgumentException.class, () -> new AlgorithmEvent.CandidateAdded(
        " ", "a", new AlgorithmEvent.ValueRef(1)));
    assertThrows(NullPointerException.class, () -> new AlgorithmEvent.CandidateAdded(
        "f", "a", null));
    assertThrows(IllegalArgumentException.class, () -> new AlgorithmEvent.CallEntered(
        "a", "a", "x"));
    assertThrows(IllegalArgumentException.class, () -> new AlgorithmEvent.CallEntered(
        "a", null, " "));
  }
}
