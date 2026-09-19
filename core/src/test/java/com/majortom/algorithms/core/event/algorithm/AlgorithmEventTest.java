package com.majortom.algorithms.core.event.algorithm;

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
import com.majortom.algorithms.core.runtime.AlgorithmEvents;
import com.majortom.algorithms.core.runtime.StatisticsReducer;
import com.majortom.algorithms.core.statistics.MetricKeys;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AlgorithmEventTest {
  private record MemoCalculated(String memo) implements AlgorithmEvent {}

  @Test
  void searchAndCacheEventsStayOnTheOrderedObservationStream() {
    InMemoryEventSink sink = new InMemoryEventSink();
    var result = new ExecutionRuntime().execute("test.search", sink, () -> {
      AlgorithmEvents.searchStarted("lookup", new AlgorithmEvent.ValueRef("needle"));
      AlgorithmEvents.searchProbed("lookup", new AlgorithmEvent.IndexRef("array", 0));
      AlgorithmEvents.cacheMiss("memo", "0");
      AlgorithmEvents.cacheStored("memo", "0");
      AlgorithmEvents.cacheHit("memo", "0");
      AlgorithmEvents.searchFound("lookup", new AlgorithmEvent.IndexRef("array", 0));
      AlgorithmEvents.cacheEvicted("memo", "0");
      AlgorithmEvents.searchCompleted("lookup", 1);
      AlgorithmEvents.emit(new MemoCalculated("memo"));
      return 42;
    });

    List<EventEnvelope> events = sink.events();
    assertEquals(11, events.size()); // run-start, nine observations, run-completed
    for (int i = 0; i < events.size(); i++) assertEquals(i, events.get(i).sequence());
    assertEquals(42, result.output().orElseThrow());
    assertEquals(9, events.stream().filter(e -> e.event() instanceof AlgorithmEvent).count());
    assertFalse(events.stream().anyMatch(e -> e.event() instanceof StructureEvent));
    assertInstanceOf(AlgorithmEvent.SearchStarted.class, events.get(1).event());
    assertInstanceOf(AlgorithmEvent.SearchCompleted.class, events.get(8).event());
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
        () -> new AlgorithmEvent.SearchCompleted("search", -1));
    assertThrows(IllegalArgumentException.class,
        () -> new AlgorithmEvent.CacheHit(" ", "key"));
    assertThrows(IllegalArgumentException.class,
        () -> new AlgorithmEvent.CacheStored("memo", " "));
    assertThrows(NullPointerException.class,
        () -> new AlgorithmEvent.SearchProbed("search", null));
    assertTrue(new MemoCalculated("memo").metricDeltas().isEmpty());
  }
}
