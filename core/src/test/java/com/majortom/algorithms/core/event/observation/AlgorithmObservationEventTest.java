package com.majortom.algorithms.core.event.observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.core.event.structure.StructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.InMemoryEventSink;
import com.majortom.algorithms.core.runtime.Observations;
import com.majortom.algorithms.core.runtime.StatisticsReducer;
import com.majortom.algorithms.core.statistics.MetricKeys;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AlgorithmObservationEventTest {
  private record MemoCalculated(String memo) implements AlgorithmObservationEvent {}

  @Test
  void searchAndCacheEventsStayOnTheOrderedObservationStream() {
    InMemoryEventSink sink = new InMemoryEventSink();
    var result = new ExecutionRuntime().execute("test.search", sink, () -> {
      Observations.searchStarted("lookup", new ObservationEvent.ValueRef("needle"));
      Observations.searchProbed("lookup", new ObservationEvent.IndexRef("array", 0));
      Observations.cacheMiss("memo", "0");
      Observations.cacheStored("memo", "0");
      Observations.cacheHit("memo", "0");
      Observations.searchFound("lookup", new ObservationEvent.IndexRef("array", 0));
      Observations.cacheEvicted("memo", "0");
      Observations.searchCompleted("lookup", 1);
      Observations.algorithm(new MemoCalculated("memo"));
      return 42;
    });

    List<EventEnvelope> events = sink.events();
    assertEquals(11, events.size()); // run-start, nine observations, run-completed
    for (int i = 0; i < events.size(); i++) assertEquals(i, events.get(i).sequence());
    assertEquals(42, result.output().orElseThrow());
    assertEquals(9, events.stream().filter(e -> e.event() instanceof ObservationEvent).count());
    assertFalse(events.stream().anyMatch(e -> e.event() instanceof StructureEvent));
    assertInstanceOf(AlgorithmObservationEvent.SearchStarted.class, events.get(1).event());
    assertInstanceOf(AlgorithmObservationEvent.SearchCompleted.class, events.get(8).event());
    assertInstanceOf(MemoCalculated.class, events.get(9).event());

    StatisticsReducer reducer = new StatisticsReducer();
    ExecutionStatistics stats = reducer.initialState();
    for (EventEnvelope event : events) stats = reducer.reduce(stats, event);
    assertEquals(Map.of(MetricKeys.SEARCH_PROBES, 1L,
        MetricKeys.SEARCH_RESULTS, 1L, MetricKeys.CACHE_HITS, 1L,
        MetricKeys.CACHE_MISSES, 1L, MetricKeys.CACHE_STORES, 1L,
        MetricKeys.CACHE_EVICTIONS, 1L), stats.metrics());
    assertFalse(stats.metrics().containsKey(MetricKeys.NODES_VISITED));
    assertFalse(stats.metrics().containsKey(MetricKeys.MATCHES));
  }

  @Test
  void invalidFactsAreRejectedWithoutMutatingAnything() {
    assertThrows(IllegalArgumentException.class,
        () -> new AlgorithmObservationEvent.SearchCompleted("search", -1));
    assertThrows(IllegalArgumentException.class,
        () -> new AlgorithmObservationEvent.CacheHit(" ", "key"));
    assertThrows(IllegalArgumentException.class,
        () -> new AlgorithmObservationEvent.CacheStored("memo", " "));
    assertThrows(NullPointerException.class,
        () -> new AlgorithmObservationEvent.SearchProbed("search", null));
    assertTrue(new MemoCalculated("memo").metricDeltas().isEmpty());
  }
}
