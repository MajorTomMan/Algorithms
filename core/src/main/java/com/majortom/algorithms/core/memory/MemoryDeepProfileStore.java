package com.majortom.algorithms.core.memory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Latest optional JFR deep profile per stable execution scope. */
public final class MemoryDeepProfileStore {
  private final Map<ScopeKey, Entry> latest = new LinkedHashMap<>();
  private long sequence;

  public synchronized void record(MemoryDeepProfile profile) {
    Objects.requireNonNull(profile, "profile");
    ScopeKey key = new ScopeKey(profile.domain(), profile.scopeId());
    latest.put(key, new Entry(++sequence, profile));
  }

  public synchronized Optional<MemoryDeepProfile> latest(MemoryDomain domain, String scopeId) {
    Entry entry = latest.get(new ScopeKey(domain, scopeId));
    return entry == null ? Optional.empty() : Optional.of(entry.profile());
  }

  /** Returns the most recently recorded profile whose stable scope id starts with {@code prefix}. */
  public synchronized Optional<MemoryDeepProfile> latestByScopePrefix(MemoryDomain domain, String prefix) {
    Objects.requireNonNull(domain, "domain");
    String normalized = Objects.requireNonNull(prefix, "prefix");
    Entry newest = null;
    for (Map.Entry<ScopeKey, Entry> candidate : latest.entrySet()) {
      ScopeKey key = candidate.getKey();
      Entry value = candidate.getValue();
      if (key.domain() == domain && key.scopeId().startsWith(normalized)
          && (newest == null || value.sequence() > newest.sequence())) {
        newest = value;
      }
    }
    return newest == null ? Optional.empty() : Optional.of(newest.profile());
  }

  public synchronized void clear() {
    latest.clear();
  }

  private record ScopeKey(MemoryDomain domain, String scopeId) {
    private ScopeKey {
      domain = Objects.requireNonNull(domain, "domain");
      scopeId = Objects.requireNonNull(scopeId, "scopeId");
    }
  }

  private record Entry(long sequence, MemoryDeepProfile profile) {}
}
