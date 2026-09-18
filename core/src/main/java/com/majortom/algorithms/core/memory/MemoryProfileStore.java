package com.majortom.algorithms.core.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Bounded in-memory history of completed profiles, grouped by domain and stable scope id. */
public final class MemoryProfileStore {
  public static final int DEFAULT_HISTORY_PER_SCOPE = 32;

  private final int historyPerScope;
  private final Map<ScopeKey, ArrayDeque<MemoryProfile>> history = new LinkedHashMap<>();

  public MemoryProfileStore() {
    this(DEFAULT_HISTORY_PER_SCOPE);
  }

  public MemoryProfileStore(int historyPerScope) {
    if (historyPerScope < 1) {
      throw new IllegalArgumentException("historyPerScope must be positive");
    }
    this.historyPerScope = historyPerScope;
  }

  public synchronized void record(MemoryProfile profile) {
    Objects.requireNonNull(profile, "profile");
    if (!profile.complete()) {
      throw new IllegalArgumentException("Only completed profiles can be stored");
    }
    ScopeKey key = new ScopeKey(profile.sessionId().domain(), profile.sessionId().scopeId());
    ArrayDeque<MemoryProfile> values = history.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    values.addLast(profile);
    while (values.size() > historyPerScope) {
      values.removeFirst();
    }
  }

  public synchronized Optional<MemoryProfile> latest(MemoryDomain domain, String scopeId) {
    ArrayDeque<MemoryProfile> values = history.get(new ScopeKey(domain, scopeId));
    return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.getLast());
  }

  /** Returns the newest completed profile whose stable scope id starts with {@code prefix}. */
  public synchronized Optional<MemoryProfile> latestByScopePrefix(MemoryDomain domain, String prefix) {
    Objects.requireNonNull(domain, "domain");
    String normalized = Objects.requireNonNull(prefix, "prefix");
    MemoryProfile newest = null;
    for (Map.Entry<ScopeKey, ArrayDeque<MemoryProfile>> candidate : history.entrySet()) {
      ScopeKey key = candidate.getKey();
      ArrayDeque<MemoryProfile> profiles = candidate.getValue();
      if (key.domain() != domain || !key.scopeId().startsWith(normalized) || profiles.isEmpty()) {
        continue;
      }
      MemoryProfile value = profiles.getLast();
      if (newest == null || value.sessionId().sequence() > newest.sessionId().sequence()) {
        newest = value;
      }
    }
    return Optional.ofNullable(newest);
  }

  public synchronized List<MemoryProfile> history(MemoryDomain domain, String scopeId) {
    ArrayDeque<MemoryProfile> values = history.get(new ScopeKey(domain, scopeId));
    return values == null ? List.of() : List.copyOf(values);
  }

  public synchronized List<MemoryProfile> allLatest() {
    List<MemoryProfile> values = new ArrayList<>(history.size());
    for (ArrayDeque<MemoryProfile> profiles : history.values()) {
      if (!profiles.isEmpty()) {
        values.add(profiles.getLast());
      }
    }
    return List.copyOf(values);
  }

  public synchronized void clear() {
    history.clear();
  }

  private record ScopeKey(MemoryDomain domain, String scopeId) {
    private ScopeKey {
      domain = Objects.requireNonNull(domain, "domain");
      scopeId = Objects.requireNonNull(scopeId, "scopeId");
    }
  }
}
