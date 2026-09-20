package com.majortom.algorithms.telemetry.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.telemetry.api.TelemetryMetricDescriptor;
import com.majortom.algorithms.telemetry.api.TelemetryMetricKind;
import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import com.majortom.algorithms.telemetry.api.TelemetryValue;
import com.majortom.algorithms.telemetry.probe.TelemetryProbe;
import com.majortom.algorithms.telemetry.probe.TelemetryProbeSession;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TelemetryCapacityTest {
  @Test
  void longSessionCompactsSamplesAndPreservesFirstLastAndSummary() {
    AtomicLong sampleNumber = new AtomicLong();
    TelemetryProbe probe = new TelemetryProbe() {
      @Override public String id() { return "counter"; }
      @Override public List<TelemetryMetricDescriptor> descriptors() {
        return List.of(new TelemetryMetricDescriptor("sample", "count", TelemetryMetricKind.CUMULATIVE));
      }
      @Override public TelemetryProbeSession open(com.majortom.algorithms.telemetry.api.TelemetrySessionId id) {
        return new TelemetryProbeSession() {
          @Override public Map<String, TelemetryValue> sample() {
            return Map.of("sample", TelemetryValue.of(sampleNumber.incrementAndGet()));
          }
          @Override public Map<String, TelemetryValue> summary() {
            return Map.of("sample", TelemetryValue.of(sampleNumber.get()));
          }
        };
      }
    };
    try (DefaultTelemetryFramework framework = new DefaultTelemetryFramework(List.of(probe), 60_000L, 4)) {
      var session = framework.begin(TelemetryScopeId.algorithm("array", "count"));
      for (int n = 0; n < 20; n++) session.sampleNow();
      session.complete();
      TelemetryProfile profile = session.snapshot();
      assertTrue(profile.samples().size() <= 4);
      assertEquals(TelemetryValue.of(1L), profile.samples().getFirst().values().get("sample"));
      assertEquals(TelemetryValue.of(sampleNumber.get()), profile.samples().getLast().values().get("sample"));
      assertEquals(TelemetryValue.of(sampleNumber.get()), profile.summaryValue("sample").orElseThrow());
    }
  }

  @Test
  void globalStoreLimitEvictsOldestAndDropsEmptyScope() {
    try (DefaultTelemetryFramework framework = new DefaultTelemetryFramework(List.of(), 60_000L)) {
      TelemetryStore store = new TelemetryStore(2, 3);
      TelemetryScopeId a = TelemetryScopeId.algorithm("array", "a");
      TelemetryScopeId b = TelemetryScopeId.algorithm("array", "b");
      TelemetryScopeId c = TelemetryScopeId.algorithm("array", "c");
      TelemetryScopeId d = TelemetryScopeId.algorithm("array", "d");
      for (TelemetryScopeId scope : List.of(a, b, c, d)) {
        var session = framework.begin(scope);
        session.complete();
        store.record(session.snapshot());
      }
      assertTrue(store.latest(a).isEmpty());
      assertTrue(store.latest(b).isPresent());
      assertEquals(3, store.allLatest().size());
      store.record(completed(framework, d));
      store.record(completed(framework, d));
      assertEquals(2, store.history(d).size());
      assertFalse(store.latest(d).isEmpty());
      store.clear();
      assertTrue(store.allLatest().isEmpty());
    }
  }

  private static TelemetryProfile completed(DefaultTelemetryFramework framework, TelemetryScopeId scope) {
    var session = framework.begin(scope);
    session.complete();
    return session.snapshot();
  }
}
