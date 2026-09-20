package com.majortom.algorithms.telemetry.analysis;

import com.majortom.algorithms.telemetry.api.TelemetryDomain;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Iterator;

/** Latest asynchronous analysis result per analyzer and stable telemetry scope. */
public final class TelemetryAnalysisStore {
  public static final int DEFAULT_MAXIMUM_ANALYSES = 256;
  private final int maximumAnalyses;
  private final Map<Key, Entry> latest = new LinkedHashMap<>();

  public TelemetryAnalysisStore() { this(DEFAULT_MAXIMUM_ANALYSES); }

  public TelemetryAnalysisStore(int maximumAnalyses) {
    if (maximumAnalyses < 1) throw new IllegalArgumentException("maximumAnalyses must be positive");
    this.maximumAnalyses = maximumAnalyses;
  }
  private long sequence;

  public synchronized void record(TelemetryAnalysisResult result) {
    Objects.requireNonNull(result, "result");
    Key key = new Key(result.analyzerId(), result.sessionId().scope());
    latest.remove(key);
    latest.put(key, new Entry(++sequence, result));
    while (latest.size() > maximumAnalyses) {
      Iterator<Key> iterator = latest.keySet().iterator();
      iterator.next();
      iterator.remove();
    }
  }

  public synchronized <R extends TelemetryAnalysisResult> Optional<R> latest(
      String analyzerId, TelemetryScopeId scope, Class<R> type) {
    Objects.requireNonNull(type, "type");
    Entry entry = latest.get(new Key(analyzerId, scope));
    if (entry == null || !type.isInstance(entry.result())) return Optional.empty();
    return Optional.of(type.cast(entry.result()));
  }

  public synchronized <R extends TelemetryAnalysisResult> Optional<R> latestByComponent(
      String analyzerId, TelemetryDomain domain, String componentId, Class<R> type) {
    Objects.requireNonNull(domain, "domain");
    Objects.requireNonNull(componentId, "componentId");
    Objects.requireNonNull(type, "type");
    Entry newest = null;
    for (Map.Entry<Key, Entry> candidate : latest.entrySet()) {
      Key key = candidate.getKey();
      TelemetryScopeId scope = key.scope();
      if (!key.analyzerId().equals(analyzerId)
          || scope.domain() != domain
          || !scope.componentId().equals(componentId)
          || !type.isInstance(candidate.getValue().result())) {
        continue;
      }
      if (newest == null || candidate.getValue().sequence() > newest.sequence()) {
        newest = candidate.getValue();
      }
    }
    return newest == null ? Optional.empty() : Optional.of(type.cast(newest.result()));
  }

  /** Drop results whose authoritative execution scope is no longer retained. */
  public synchronized void retainScopes(Set<TelemetryScopeId> retained) {
    Objects.requireNonNull(retained, "retained");
    latest.keySet().removeIf(key -> !retained.contains(key.scope()));
  }

  public synchronized void discardScope(TelemetryScopeId scope) {
    Objects.requireNonNull(scope, "scope");
    latest.keySet().removeIf(key -> key.scope().equals(scope));
  }

  public synchronized void clear() {
    latest.clear();
  }

  private record Key(String analyzerId, TelemetryScopeId scope) {
    private Key {
      analyzerId = Objects.requireNonNull(analyzerId, "analyzerId");
      scope = Objects.requireNonNull(scope, "scope");
    }
  }

  private record Entry(long sequence, TelemetryAnalysisResult result) {}
}
