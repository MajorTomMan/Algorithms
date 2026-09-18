package com.majortom.algorithms.telemetry.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class DefaultTelemetryFrameworkTest {
  @Test
  void oneSamplerCombinesProbeFactsAndStoreIsScopeIsolated() {
    AtomicLong value = new AtomicLong();
    TelemetryProbe probe = new TelemetryProbe() {
      @Override
      public String id() {
        return "fake";
      }

      @Override
      public List<TelemetryMetricDescriptor> descriptors() {
        return List.of(new TelemetryMetricDescriptor(
            "fake.total", "bytes", TelemetryMetricKind.CUMULATIVE));
      }

      @Override
      public TelemetryProbeSession open(com.majortom.algorithms.telemetry.api.TelemetrySessionId id) {
        return new TelemetryProbeSession() {
          @Override
          public Map<String, TelemetryValue> sample() {
            return Map.of("fake.total", TelemetryValue.of(value.addAndGet(10L)));
          }
        };
      }
    };

    try (DefaultTelemetryFramework framework = new DefaultTelemetryFramework(List.of(probe), 60_000L)) {
      TelemetryScopeId scope = TelemetryScopeId.algorithm("array", "bubble-sort");
      TelemetrySession session = framework.begin(scope);
      session.sampleNow();
      session.sampleNow();
      session.complete();
      TelemetryProfile profile = session.snapshot();

      assertEquals(scope, profile.sessionId().scope());
      assertTrue(profile.samples().size() >= 3);
      assertTrue(profile.summaryValue("fake.total").isPresent());

      TelemetryStore store = new TelemetryStore(2);
      store.record(profile);
      assertEquals(profile, store.latest(scope).orElseThrow());
      assertTrue(store.latest(TelemetryScopeId.algorithm("array", "quick-sort")).isEmpty());
    }
  }

  @Test
  void stableScopeIdentityDoesNotDependOnControllerClass() {
    assertEquals(
        "algorithm/tree/inorder",
        TelemetryScopeId.algorithm("tree", "inorder").stableId());
    assertEquals(
        "algorithm/avl-tree/inorder",
        TelemetryScopeId.algorithm("avl-tree", "inorder").stableId());
  }
}
