package com.majortom.algorithms.core.event.observation;

import com.majortom.algorithms.core.statistics.MetricKeys;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only algorithm facts, reusable across different Structure implementations.
 *
 * <p>The EventEnvelope supplies the algorithm/run identity and the global event order. The
 * searchId/cacheId fields identify a logical search or memo table <em>inside</em> that run.
 * Neither these events nor custom implementations may be interpreted as structure mutations.
 * Presenters may use them to render algorithm-only overlays or derive algorithm statistics.
 *
 * <p>When a Reference contains a ValueRef, the value must satisfy the project's snapshot
 * immutability/freeze contract before historical replay is enabled. Prefer stable IndexRef,
 * EntityRef and CoordinateRef for locations; use an immutable identifier for cache keys.
 */
public non-sealed interface AlgorithmObservationEvent extends ObservationEvent {
  /** Algorithm-specific events need not define a metric. */
  @Override
  default Map<String, Long> metricDeltas() {
    return Map.of();
  }

  /** An algorithm begins one logical search; the target is a fact, not a structure command. */
  record SearchStarted(String searchId, ObservationEvent.Reference target)
      implements AlgorithmObservationEvent {
    public SearchStarted {
      searchId = requireName(searchId, "searchId");
      Objects.requireNonNull(target, "target");
    }
  }

  /** A candidate was inspected; this does not imply a visit or a comparison. */
  record SearchProbed(String searchId, ObservationEvent.Reference candidate)
      implements AlgorithmObservationEvent {
    public SearchProbed {
      searchId = requireName(searchId, "searchId");
      Objects.requireNonNull(candidate, "candidate");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.SEARCH_PROBES, 1L);
    }
  }

  /** A logical search produced one match. Existing Matched/Visited counters are unaffected. */
  record SearchFound(String searchId, ObservationEvent.Reference result)
      implements AlgorithmObservationEvent {
    public SearchFound {
      searchId = requireName(searchId, "searchId");
      Objects.requireNonNull(result, "result");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.SEARCH_RESULTS, 1L);
    }
  }

  /** Terminal search fact: zero results means "not found" without inventing a missing node. */
  record SearchCompleted(String searchId, long resultCount)
      implements AlgorithmObservationEvent {
    public SearchCompleted {
      searchId = requireName(searchId, "searchId");
      if (resultCount < 0L) throw new IllegalArgumentException("resultCount must not be negative");
    }
  }

  /** An algorithm-internal memo/cache lookup found a value; no structure read is implied. */
  record CacheHit(String cacheId, String key) implements AlgorithmObservationEvent {
    public CacheHit {
      cacheId = requireName(cacheId, "cacheId");
      key = requireName(key, "key");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.CACHE_HITS, 1L);
    }
  }

  record CacheMiss(String cacheId, String key) implements AlgorithmObservationEvent {
    public CacheMiss {
      cacheId = requireName(cacheId, "cacheId");
      key = requireName(key, "key");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.CACHE_MISSES, 1L);
    }
  }

  /** A cache entry was added or replaced. Does not update the underlying Structure. */
  record CacheStored(String cacheId, String key) implements AlgorithmObservationEvent {
    public CacheStored {
      cacheId = requireName(cacheId, "cacheId");
      key = requireName(key, "key");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.CACHE_STORES, 1L);
    }
  }

  /** An algorithm evicted an entry from its own memo/cache, not from the Structure. */
  record CacheEvicted(String cacheId, String key) implements AlgorithmObservationEvent {
    public CacheEvicted {
      cacheId = requireName(cacheId, "cacheId");
      key = requireName(key, "key");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.CACHE_EVICTIONS, 1L);
    }
  }


  /** A candidate enters a logical algorithm frontier. candidateId distinguishes equal values. */
  record CandidateAdded(String frontierId, String candidateId, ObservationEvent.Reference ref)
      implements AlgorithmObservationEvent {
    @Override public Map<String, Long> metricDeltas() { return Map.of(MetricKeys.FRONTIER_ADDITIONS, 1L); }
    public CandidateAdded {
      frontierId = requireName(frontierId, "frontierId");
      candidateId = requireName(candidateId, "candidateId");
      Objects.requireNonNull(ref, "ref");
    }
  }

  /** Selection does not imply removal from the frontier or a structure read. */
  record CandidateSelected(String frontierId, String candidateId)
      implements AlgorithmObservationEvent {
    @Override public Map<String, Long> metricDeltas() { return Map.of(MetricKeys.FRONTIER_SELECTIONS, 1L); }
    public CandidateSelected {
      frontierId = requireName(frontierId, "frontierId");
      candidateId = requireName(candidateId, "candidateId");
    }
  }

  /** Candidate rejected without mutation of its underlying structure. */
  record CandidateRejected(String frontierId, String candidateId)
      implements AlgorithmObservationEvent {
    @Override public Map<String, Long> metricDeltas() { return Map.of(MetricKeys.FRONTIER_REJECTIONS, 1L); }
    public CandidateRejected {
      frontierId = requireName(frontierId, "frontierId");
      candidateId = requireName(candidateId, "candidateId");
    }
  }

  /** Candidate discarded by a branch-and-bound/backtracking decision. */
  record CandidatePruned(String frontierId, String candidateId)
      implements AlgorithmObservationEvent {
    @Override public Map<String, Long> metricDeltas() { return Map.of(MetricKeys.FRONTIER_PRUNES, 1L); }
    public CandidatePruned {
      frontierId = requireName(frontierId, "frontierId");
      candidateId = requireName(candidateId, "candidateId");
    }
  }

  /** A logical recursive call is entered. parentCallId is null only for a root call. */
  record CallEntered(String callId, String parentCallId, String label)
      implements AlgorithmObservationEvent {
    @Override public Map<String, Long> metricDeltas() { return Map.of(MetricKeys.RECURSIVE_CALLS, 1L); }
    public CallEntered {
      callId = requireName(callId, "callId");
      if (parentCallId != null) parentCallId = requireName(parentCallId, "parentCallId");
      label = requireName(label, "label");
      if (callId.equals(parentCallId)) throw new IllegalArgumentException("call cannot parent itself");
    }
  }

  /** Return of the most recent logical call, not the mutation of a StackStructure. */
  record CallReturned(String callId, String resultSummary)
      implements AlgorithmObservationEvent {
    @Override public Map<String, Long> metricDeltas() { return Map.of(MetricKeys.RECURSIVE_RETURNS, 1L); }
    public CallReturned {
      callId = requireName(callId, "callId");
      resultSummary = Objects.requireNonNull(resultSummary, "resultSummary");
    }
  }

  private static String requireName(String value, String parameter) {
    Objects.requireNonNull(value, parameter);
    if (value.isBlank()) throw new IllegalArgumentException(parameter + " must not be blank");
    return value;
  }
}
