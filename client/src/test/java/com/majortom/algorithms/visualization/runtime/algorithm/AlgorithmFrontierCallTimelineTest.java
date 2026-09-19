package com.majortom.algorithms.visualization.runtime.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgorithmFrontierCallTimelineTest {
  private static EventEnvelope event(String run, int sequence,
      com.majortom.algorithms.core.event.ExecutionEvent fact) {
    return new EventEnvelope(run, "algorithm", sequence, Instant.EPOCH, "test", fact);
  }

  @Test
  void seekReconstructsCandidateAndNestedCallStateWithoutChangingSearchOrCache() {
    List<EventEnvelope> events = List.of(
        event("a", 0, new AlgorithmEvent.SearchStarted("s", new AlgorithmEvent.ValueRef("needle"))),
        event("a", 1, new AlgorithmEvent.CandidateAdded("f", "n1", new AlgorithmEvent.IndexRef("arr", 1))),
        event("a", 2, new AlgorithmEvent.CandidateAdded("f", "n2", new AlgorithmEvent.IndexRef("arr", 2))),
        event("a", 3, new AlgorithmEvent.CandidateSelected("f", "n1")),
        event("a", 4, new AlgorithmEvent.CallEntered("root", null, "f(1)")),
        event("a", 5, new AlgorithmEvent.CallEntered("child", "root", "f(2)")),
        event("a", 6, new AlgorithmEvent.CandidatePruned("f", "n2")),
        event("a", 7, new AlgorithmEvent.CallReturned("child", "2")),
        event("a", 8, new AlgorithmEvent.CallReturned("root", "3")));
    var timeline = new AlgorithmObservationTimeline();
    var middle = timeline.at(events, 5);
    assertEquals(2, middle.callStack().size());
    assertEquals("child", middle.callStack().getLast().id());
    assertEquals(AlgorithmObservationModel.CandidateStatus.SELECTED,
        middle.currentFrontier().candidates().get("n1").status());
    assertEquals("needle", middle.currentSearch().target());
    assertEquals(AlgorithmObservationModel.CandidateStatus.PRUNED,
        timeline.at(events, 6).currentFrontier().candidates().get("n2").status());
    assertTrue(timeline.at(events, 8).callStack().isEmpty());
    assertEquals(2, timeline.at(events, 5).callStack().size());
    assertTrue(timeline.at(events, 8).callStack().isEmpty());
    var next = timeline.at(List.of(event("b", 0,
        new AlgorithmEvent.CallEntered("other", null, "different"))), 0);
    assertTrue(next.frontiers().isEmpty());
    assertNull(next.currentSearch());
    assertEquals(1, next.callStack().size());
  }

  @Test
  void invalidOutOfOrderReturnDoesNotLoseExistingFrames() {
    var first = AlgorithmObservationModel.apply(AlgorithmObservationModel.empty(),
        event("r", 0, new AlgorithmEvent.CallEntered("root", null, "root")));
    var invalid = AlgorithmObservationModel.apply(first,
        event("r", 1, new AlgorithmEvent.CallReturned("missing", "")));
    assertEquals(first.callStack(), invalid.callStack());
    assertEquals(AlgorithmObservationModel.Kind.CALL_INVALID, invalid.pulse().kind());
  }
}
