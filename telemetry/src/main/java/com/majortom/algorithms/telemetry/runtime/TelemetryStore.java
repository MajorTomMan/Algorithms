package com.majortom.algorithms.telemetry.runtime;

import com.majortom.algorithms.telemetry.api.TelemetryDomain;
import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import com.majortom.algorithms.telemetry.api.TelemetrySessionId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 独立于控制器实例的有界遥测历史存储。
 *
 * <p>按稳定的 TelemetryScopeId 聚合终态 TelemetryProfile，
 * 同时限制每个 scope 保留数量和全局上限，避免长时间运行工作台导致历史无限增长。
 * 只接受已完成/失败/取消等可存储终态，避免把未结束会话混入历史比较。
 *
 * <p>无需依赖 JavaFX、Graph 或算法实现，可用于不同模块的性能分析。
 */
public final class TelemetryStore {
  public static final int DEFAULT_HISTORY_PER_SCOPE = 10;
  public static final int DEFAULT_MAXIMUM_PROFILES = 256;

  private final int historyPerScope;
  private final int maximumProfiles;
  private final ArrayDeque<StoredProfile> insertionOrder = new ArrayDeque<>();
  private final Map<TelemetryScopeId, ArrayDeque<TelemetryProfile>> history = new LinkedHashMap<>();

  public TelemetryStore() {
    this(DEFAULT_HISTORY_PER_SCOPE);
  }

  public TelemetryStore(int historyPerScope) {
    this(historyPerScope, Math.max(DEFAULT_MAXIMUM_PROFILES, historyPerScope));
  }

  public TelemetryStore(int historyPerScope, int maximumProfiles) {
    if (maximumProfiles < 1) {
      throw new IllegalArgumentException("maximumProfiles must be positive");
    }
    this.maximumProfiles = maximumProfiles;
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
    insertionOrder.addLast(new StoredProfile(scope, profile));
    while (values.size() > historyPerScope) {
      TelemetryProfile removed = values.removeFirst();
      insertionOrder.removeIf(entry -> entry.profile == removed);
    }
    while (insertionOrder.size() > maximumProfiles) {
      StoredProfile oldest = insertionOrder.removeFirst();
      ArrayDeque<TelemetryProfile> scopeHistory = history.get(oldest.scope);
      scopeHistory.removeFirst();
      if (scopeHistory.isEmpty()) history.remove(oldest.scope);
    }
  }

  public synchronized Optional<TelemetryProfile> latest(TelemetryScopeId scope) {
    ArrayDeque<TelemetryProfile> values = history.get(Objects.requireNonNull(scope, "scope"));
    return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.getLast());
  }

  /** Returns the newest terminal profile for one stable component across its execution ids. */
  public synchronized Optional<TelemetryProfile> latestByComponent(
      TelemetryDomain domain, String componentId) {
    Objects.requireNonNull(domain, "domain");
    Objects.requireNonNull(componentId, "componentId");
    TelemetryProfile newest = null;
    for (Map.Entry<TelemetryScopeId, ArrayDeque<TelemetryProfile>> candidate : history.entrySet()) {
      TelemetryScopeId scope = candidate.getKey();
      ArrayDeque<TelemetryProfile> profiles = candidate.getValue();
      if (scope.domain() != domain || !scope.componentId().equals(componentId) || profiles.isEmpty()) {
        continue;
      }
      TelemetryProfile value = profiles.getLast();
      if (newest == null || value.sessionId().sequence() > newest.sessionId().sequence()) {
        newest = value;
      }
    }
    return Optional.ofNullable(newest);
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

  /** Current retained scopes; analysis data may exist only for these scopes. */
  public synchronized Set<TelemetryScopeId> retainedScopes() {
    return Set.copyOf(history.keySet());
  }

  /** Prevents asynchronous analysis results from reviving a superseded execution. */
  public synchronized boolean isLatestSession(TelemetrySessionId id) {
    ArrayDeque<TelemetryProfile> values = history.get(Objects.requireNonNull(id, "id").scope());
    return values != null && !values.isEmpty() && values.getLast().sessionId().equals(id);
  }

  public synchronized void clear() {
    history.clear();
    insertionOrder.clear();
  }

  private static final class StoredProfile {
    final TelemetryScopeId scope;
    final TelemetryProfile profile;

    StoredProfile(TelemetryScopeId scope, TelemetryProfile profile) {
      this.scope = scope;
      this.profile = profile;
    }
  }
}
