package com.majortom.algorithms.visualization.runtime.algorithm;

import static org.junit.jupiter.api.Assertions.*;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgorithmObservationTimelineTest {
  private static EventEnvelope envelope(String run, int seq,
      com.majortom.algorithms.core.event.ExecutionEvent event) {
    return new EventEnvelope(run, "algorithm", seq, Instant.EPOCH, "test", event);
  }

  @Test
  void searchAndCacheReplayAreIndependentAndSeekable() {
    List<EventEnvelope> events = List.of(
        envelope("a", 0, new AlgorithmEvent.SearchStarted("search", new AlgorithmEvent.ValueRef("target"))),
        envelope("a", 1, new AlgorithmEvent.SearchProbed("search", new AlgorithmEvent.IndexRef("array", 2))),
        envelope("a", 2, new AlgorithmEvent.SearchFound("search", new AlgorithmEvent.IndexRef("array", 2))),
        envelope("a", 3, new AlgorithmEvent.CacheMiss("memo", "k")),
        envelope("a", 4, new AlgorithmEvent.CacheStored("memo", "k")),
        envelope("a", 5, new AlgorithmEvent.Visited(new AlgorithmEvent.EntityRef("node", 1))),
        envelope("a", 6, new AlgorithmEvent.CacheHit("memo", "k")),
        envelope("a", 7, new AlgorithmEvent.SearchCompleted("search", 1)));
    AlgorithmObservationTimeline timeline = new AlgorithmObservationTimeline();
    var last = timeline.at(events, 7);
    assertEquals(1, last.currentSearch().resultCount());
    assertEquals(1, last.currentSearch().probes());
    assertTrue(last.currentSearch().completed());
    assertEquals(AlgorithmObservationModel.Kind.SEARCH_COMPLETED, last.pulse().kind());
    var beforeFound = timeline.at(events, 1);
    assertEquals(0, beforeFound.currentSearch().resultCount());
    assertEquals("array[2]", beforeFound.currentSearch().candidate());
    assertTrue(beforeFound.recentCache().isEmpty());
    var ignored = timeline.at(events, 5);
    assertEquals(AlgorithmObservationModel.Kind.NONE, ignored.pulse().kind());
    assertEquals(AlgorithmObservationModel.Kind.CACHE_STORED, ignored.recentCache().getFirst().lastAction());
    assertEquals(last, timeline.at(events, 7));
    assertNull(timeline.at(events, -1).currentSearch());
  }

  @Test
  void differentRunAndChangedEventPrefixDoNotLeakOldState() {
    AlgorithmObservationTimeline timeline = new AlgorithmObservationTimeline();
    List<EventEnvelope> a = List.of(envelope("a", 0,
        new AlgorithmEvent.CacheStored("memo", "old")));
    assertTrue(timeline.at(a, 0).hasContent());
    List<EventEnvelope> b = List.of(envelope("b", 0,
        new AlgorithmEvent.SearchStarted("s", new AlgorithmEvent.ValueRef("new"))));
    var state = timeline.at(b, 0);
    assertEquals("b", state.runId());
    assertTrue(state.recentCache().isEmpty());
    assertEquals("new", state.currentSearch().target());
  }

  @Test
  void recentCacheIsBoundedAndDoesNotPretendToBeTheActualCache() {
    List<EventEnvelope> events = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      events.add(envelope("run", i, new AlgorithmEvent.CacheStored("memo", "key-" + i)));
    }
    events.add(envelope("run", 20, new AlgorithmEvent.CacheEvicted("memo", "key-19")));
    var last = new AlgorithmObservationTimeline().at(events, 20);
    assertEquals(AlgorithmObservationModel.MAX_RECENT_CACHE, last.recentCache().size());
    assertEquals("key-19", last.recentCache().getFirst().key());
    assertEquals(AlgorithmObservationModel.Kind.CACHE_EVICTED, last.recentCache().getFirst().lastAction());
  }
}
