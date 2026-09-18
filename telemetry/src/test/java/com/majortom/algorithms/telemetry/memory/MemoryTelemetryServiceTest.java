package com.majortom.algorithms.telemetry.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.telemetry.api.TelemetryDomain;
import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import com.majortom.algorithms.telemetry.memory.api.MemoryFacts;
import com.majortom.algorithms.telemetry.memory.runtime.MemoryTelemetryRun;
import com.majortom.algorithms.telemetry.memory.runtime.MemoryTelemetryService;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryTelemetryServiceTest {
  @Test
  void capturesExecutionFactsAndStoresThemByStableScope() throws Exception {
    try (MemoryTelemetryService telemetry = new MemoryTelemetryService(5L)) {
      TelemetryScopeId scope = TelemetryScopeId.algorithm("array", "bubble-sort");
      MemoryTelemetryRun run = telemetry.begin(scope, false);
      List<byte[]> retained = new ArrayList<>();
      for (int index = 0; index < 32; index++) {
        retained.add(new byte[8 * 1024]);
      }
      Thread.sleep(15L);

      TelemetryProfile live = run.snapshot();
      assertFalse(live.state().executionEnded());
      run.complete();
      TelemetryProfile completed = run.snapshot();
      MemoryFacts facts = MemoryFacts.from(completed);

      assertTrue(completed.state().executionEnded());
      assertEquals(scope, completed.sessionId().scope());
      assertTrue(completed.durationNanos() > 0L);
      assertEquals(completed, telemetry.latest(scope).orElseThrow());
      assertNotNull(telemetry.capabilities());
      if (telemetry.capabilities().threadAllocatedBytes()) {
        assertTrue(facts.allocatedBytesValue().isPresent());
        assertTrue(facts.allocatedBytesValue().getAsLong() >= 256 * 1024L);
        assertFalse(facts.samples().isEmpty());
      }

      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
        output.writeObject(completed);
      }
      assertTrue(bytes.size() > 0);
      assertEquals(32, retained.size());
    }
  }

  @Test
  void latestByComponentKeepsStructureVariantsIsolated() {
    try (MemoryTelemetryService telemetry = new MemoryTelemetryService(60_000L)) {
      recordCompleted(telemetry, TelemetryScopeId.structure("tree", "add-child"));
      recordCompleted(telemetry, TelemetryScopeId.structure("avl-tree", "insert"));
      recordCompleted(telemetry, TelemetryScopeId.structure("tree", "delete"));

      assertEquals(
          "delete",
          telemetry.latestByComponent(TelemetryDomain.STRUCTURE, "tree")
              .orElseThrow().sessionId().scope().executionId());
      assertEquals(
          "insert",
          telemetry.latestByComponent(TelemetryDomain.STRUCTURE, "avl-tree")
              .orElseThrow().sessionId().scope().executionId());
    }
  }

  @Test
  void debuggerTimingSuppressionPreservesSummaryAndDropsTimeline() {
    try (MemoryTelemetryService telemetry = new MemoryTelemetryService(60_000L)) {
      MemoryTelemetryRun run = telemetry.begin(TelemetryScopeId.practice("probe"), false);
      run.complete();
      TelemetryProfile original = run.snapshot();
      TelemetryProfile suppressed = original.withoutRepresentativeTiming();

      assertFalse(suppressed.timingRepresentative());
      assertTrue(suppressed.samples().isEmpty());
      assertEquals(original.summary(), suppressed.summary());
      assertEquals(original.sessionId(), suppressed.sessionId());
    }
  }

  private static void recordCompleted(MemoryTelemetryService telemetry, TelemetryScopeId scope) {
    MemoryTelemetryRun run = telemetry.begin(scope, false);
    run.complete();
  }
}
