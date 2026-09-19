package com.majortom.algorithms.visualization.runtime.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.event.structure.ArrayStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgorithmObservationCalloutTest {
  private static EventEnvelope event(String run, int sequence, ExecutionEvent fact) {
    return new EventEnvelope(run, "algorithm", sequence, Instant.EPOCH, "test", fact);
  }

  record CustomDecision(String note) implements AlgorithmEvent {}

  @Test
  void pruningUsesRecordedCandidateReferenceAndNeverInfersReason() {
    List<EventEnvelope> events = List.of(
        event("run", 0, new AlgorithmEvent.CandidateAdded("frontier", "node-7",
            new AlgorithmEvent.EntityRef("vertex", 7))),
        event("run", 1, new AlgorithmEvent.CandidatePruned("frontier", "node-7")));
    AlgorithmObservationTimeline timeline = new AlgorithmObservationTimeline();
    var callout = AlgorithmObservationCallout.at(events.get(1), timeline.at(events, 1), 1);
    assertEquals("pruned", callout.category());
    assertEquals("label.algorithm.observation.candidate_pruned", callout.titleKey());
    assertEquals("node-7 · vertex #7", callout.subject());
    assertEquals("frontier", callout.detail());
    assertTrue(timeline.at(events, 0).currentFrontier().candidates().get("node-7")
        .status() == AlgorithmObservationModel.CandidateStatus.PENDING);
  }

  @Test
  void structureEventsHideTheCueButKeepTheirAuthoritativeTimelinePosition() {
    List<EventEnvelope> events = List.of(
        event("run", 0, new AlgorithmEvent.Visited(new AlgorithmEvent.IndexRef("array", 1))),
        event("run", 1, new ArrayStructureEvent.Swapped(0, 1, 4, 2)),
        event("run", 2, new AlgorithmEvent.CacheHit("memo", "x")));
    var timeline = new AlgorithmObservationTimeline();
    assertTrue(AlgorithmObservationCallout.at(events.get(0), timeline.at(events, 0), 0).visible());
    var structure = AlgorithmObservationCallout.at(events.get(1), timeline.at(events, 1), 1);
    assertFalse(structure.visible());
    assertEquals(1, structure.cursorIndex());
    assertEquals("run", structure.runId());
    assertEquals("cache", AlgorithmObservationCallout.at(events.get(2), timeline.at(events, 2), 2).category());
    assertEquals("generic", AlgorithmObservationCallout.at(events.get(0), timeline.at(events, 0), 0).category());
  }

  @Test
  void structureOrientedAlgorithmEventsGetGenericTargetedCallout() {
    var candidate = event("r", 8,
        new com.majortom.algorithms.core.event.algorithm.GraphAlgorithmEvent.EdgeAccepted(12, 1, 2));
    var detail = AlgorithmObservationCallout.at(candidate,
        new AlgorithmObservationTimeline().at(List.of(candidate), 0), 0);
    assertTrue(detail.visible());
    assertEquals("Edge Accepted", detail.titleFallback());
    assertEquals("graph.edge #12", detail.subject());
  }

  @Test
  void customAlgorithmEventsNeedNoCoreChangesAndCrossRunCuesDoNotLeak() {
    var first = event("one", 0, new CustomDecision("private detail"));
    var second = event("two", 0, new AlgorithmEvent.SearchCompleted("search", 0));
    assertEquals("Custom Decision", AlgorithmObservationCallout.at(first,
        new AlgorithmObservationTimeline().at(List.of(first), 0), 0).titleFallback());
    var other = AlgorithmObservationCallout.at(second,
        new AlgorithmObservationTimeline().at(List.of(second), 0), 0);
    assertEquals("two", other.runId());
    assertFalse(other.subject().contains("private detail"));
    assertEquals(AlgorithmObservationCallout.empty(),
        AlgorithmObservationCallout.at(null, AlgorithmObservationModel.empty(), -1));
  }
}
