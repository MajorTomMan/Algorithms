package com.majortom.algorithms.core.event.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.majortom.algorithms.core.event.structure.StructureEvent;
import com.majortom.algorithms.core.runtime.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgorithmEventRoutingTest {
  private record CustomDecision(int choice) implements AlgorithmEvent {}
  private record InvalidDualChannel() implements AlgorithmEvent, StructureEvent {}

  @Test
  void typedChannelsShareOneTimelineButNeverChangeEventClassification() {
    InMemoryEventSink sink = new InMemoryEventSink();
    var result = new ExecutionRuntime().execute("alg", sink, () -> {
      StructureEvents.arrayInserted(0, 7);
      AlgorithmEvents.visited("node", 1);
      AlgorithmEvents.emit(new GraphAlgorithmEvent.EdgeAccepted(5, 1, 2));
      AlgorithmEvents.emit(new CustomDecision(42));
      return null;
    });
    assertEquals(ExecutionStatus.COMPLETED, result.status());
    List<EventEnvelope> events = sink.events();
    assertEquals(6, events.size());
    for (int i = 0; i < events.size(); i++) assertEquals(i, events.get(i).sequence());
    assertEquals(1L, events.stream().filter(e -> e.event() instanceof StructureEvent).count());
    assertEquals(3L, events.stream().filter(e -> e.event() instanceof AlgorithmEvent).count());
    assertEquals("graph.edge", ((GraphAlgorithmEvent.EdgeAccepted) events.get(3).event()).target().domain());
  }

  @Test
  void oldUnrestrictedDomainEmissionAndAmbiguousChannelAreRejected() {
    assertThrows(IllegalArgumentException.class, () ->
        ExecutionEvents.emit(new AlgorithmEvent.Visited(new AlgorithmEvent.EntityRef("node", 1))));
    assertThrows(IllegalArgumentException.class, () ->
        ExecutionEvents.structure(new InvalidDualChannel()));
    assertThrows(IllegalArgumentException.class, () ->
        AlgorithmEvents.emit(new InvalidDualChannel()));
    assertThrows(IllegalArgumentException.class, () ->
        ExecutionEvents.emit(new GraphAlgorithmEvent.EdgeAccepted(5, 1, 2)));
  }
}
