package com.majortom.algorithms.visualization.runtime.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GenericComputationObservationTest {
  @Test
  void computedFactUsesGenericPresentationWithoutMutationOrMetric() {
    AlgorithmEvent.Computed computation = new AlgorithmEvent.Computed(
        "hashCode", new AlgorithmEvent.ValueRef(42), new AlgorithmEvent.ValueRef(42));
    EventEnvelope envelope = new EventEnvelope(
        "run-1", "generic-test", 1, Instant.EPOCH, "test", computation);

    AlgorithmObservationModel model =
        AlgorithmObservationModel.apply(AlgorithmObservationModel.empty(), envelope);
    AlgorithmObservationCallout callout = AlgorithmObservationCallout.at(envelope, model, 0);

    assertEquals(AlgorithmObservationModel.Kind.COMPUTED, model.pulse().kind());
    assertEquals("hashCode", model.pulse().scope());
    assertEquals("42 → 42", model.pulse().detail());
    assertTrue(callout.visible());
    assertEquals("label.algorithm.observation.computed", callout.titleKey());
    assertEquals("hashCode", callout.detail());
    assertTrue(computation.metricDeltas().isEmpty());
  }
}
