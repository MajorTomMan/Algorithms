package com.majortom.algorithms.telemetry.runtime;

import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Bounded session history keyed only by stable TelemetryScopeId, never by controllers or views. */
public final class TelemetryStore {
  public static final int DEFAULT_HISTORY_PER_SCOPE = 10;

  private final int historyPerScope;
  private final Map<TelemetryScopeId, ArrayDeque<TelemetryProfile>> history = new LinkedHashMap<>();

  public TelemetryStore() {
    this(DEFAULT_HISTORY_PER_SCOPE);
  }

  public TelemetryStore(int historyPerScope) {
    if (historyPerScope < 1) {
      throw new IllegalArgumentException("historyPerScope must be positive");
    }
    this.historyPerScope = historyPerScope;
  }

  public synchronized void record(TelemetryProfile profile) {
    Objects.requireNonNull(profile, "profile");
    if (!profile.state().storable()) {
      throw new IllegalArgumentException("Only terminal telemetry profiles can be stored");
    }
    TelemetryScopeId scope = profile.sessionId().scope();
    ArrayDeque<TelemetryProfile> values = history.computeIfAbsent(scope, ignored -> new ArrayDeque<>());
    values.addLast(profile);
    while (values.size() > historyPerScope) {
      values.removeFirst();
    }
  }

  public synchronized Optional<TelemetryProfile> latest(TelemetryScopeId scope) {
    ArrayDeque<TelemetryProfile> values = history.get(Objects.requireNonNull(scope, "scope"));
    return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.getLast());
  }

  public synchronized List<TelemetryProfile> history(TelemetryScopeId scope) {
    ArrayDeque<TelemetryProfile> values = history.get(Objects.requireNonNull(scope, "scope"));
    return values == null ? List.of() : List.copyOf(values);
  }

  public synchronized List<TelemetryProfile> allLatest() {
    List<TelemetryProfile> result = new ArrayList<>();
    for (ArrayDeque<TelemetryProfile> profiles : history.values()) {
      if (!profiles.isEmpty()) {
        result.add(profiles.getLast());
      }
    }
    return List.copyOf(result);
  }

  public synchronized void clear() {
    history.clear();
  }
}
