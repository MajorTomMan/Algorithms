package com.majortom.algorithms.core.event.algorithm;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.statistics.MetricKeys;
import com.majortom.algorithms.core.statistics.StatisticsContribution;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Algorithm-facing event payload. Events describe a decision or inspection; they never mutate
 * the canonical Structure. ExecutionRuntime owns run id, sequence, checkpoints and delivery.
 * Algorithms may publish their own record implementations without changing this interface.
 */
public interface AlgorithmEvent extends ExecutionEvent, StatisticsContribution {
  @Override
  default Map<String, Long> metricDeltas() { return Map.of(); }

  /** An event with one stable render target. Unknown targets may still be shown as text. */
  interface Targeted extends AlgorithmEvent {
    Reference target();
  }

  /** Marker for stable references carried by observation facts. */
  sealed interface Reference permits EntityRef, IndexRef, CoordinateRef, ValueRef {}

  record EntityRef(String domain, long id) implements Reference {
    public EntityRef {
      domain = requireText(domain, "domain");
      if (id <= 0L) {
        throw new IllegalArgumentException("entity id must be positive");
      }
    }
  }

  record IndexRef(String source, int index) implements Reference {
    public IndexRef {
      source = requireText(source, "source");
      if (index < 0) {
        throw new IllegalArgumentException("index must not be negative");
      }
    }
  }

  record CoordinateRef(int row, int column) implements Reference {
    public CoordinateRef {
      if (row < 0 || column < 0) {
        throw new IllegalArgumentException("coordinates must not be negative");
      }
    }
  }

  record ValueRef(Object value) implements Reference {
    public ValueRef {
      Objects.requireNonNull(value, "value");
    }
  }

  record Compared(Reference leftRef, Reference rightRef) implements AlgorithmEvent {
    public Compared {
      Objects.requireNonNull(leftRef, "leftRef");
      Objects.requireNonNull(rightRef, "rightRef");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.COMPARISONS, 1L);
    }
  }

  record Visited(Reference ref) implements AlgorithmEvent {
    public Visited {
      Objects.requireNonNull(ref, "ref");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.NODES_VISITED, 1L);
    }
  }

  record Examined(Reference fromRef, Reference toRef) implements AlgorithmEvent {
    public Examined {
      Objects.requireNonNull(fromRef, "fromRef");
      Objects.requireNonNull(toRef, "toRef");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.EDGES_EXAMINED, 1L);
    }
  }

  record Matched(int index, int length) implements AlgorithmEvent {
    public Matched {
      if (index < 0) {
        throw new IllegalArgumentException("match index must not be negative");
      }
      if (length <= 0) {
        throw new IllegalArgumentException("match length must be positive");
      }
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.MATCHES, 1L);
    }
  }

  record Fallback(int fromIndex, int toIndex) implements AlgorithmEvent {
    public Fallback {
      if (fromIndex < 0 || toIndex < 0) {
        throw new IllegalArgumentException("fallback indexes must not be negative");
      }
      if (toIndex > fromIndex) {
        throw new IllegalArgumentException("fallback target must not move forward");
      }
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.FALLBACKS, 1L);
    }
  }

  record Backtracked(Reference ref) implements AlgorithmEvent {
    public Backtracked {
      Objects.requireNonNull(ref, "ref");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.BACKTRACKS, 1L);
    }
  }

  /** One factual predecessor-chain step while reconstructing a discovered path. */
  record PathTraced(Reference ref) implements AlgorithmEvent {
    public PathTraced {
      Objects.requireNonNull(ref, "ref");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of();
    }
  }

  /** Factual final path, expressed only through stable domain references. */
  record PathFound(List<Reference> refs) implements AlgorithmEvent {
    public PathFound {
      refs = List.copyOf(Objects.requireNonNull(refs, "refs"));
      if (refs.isEmpty()) {
        throw new IllegalArgumentException("path refs must not be empty");
      }
      for (Reference ref : refs) Objects.requireNonNull(ref, "path ref");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of();
    }
  }

  /** An algorithm begins one logical search; the target is a fact, not a structure command. */
  record SearchStarted(String searchId, AlgorithmEvent.Reference target)
      implements AlgorithmEvent {
    public SearchStarted {
      searchId = requireName(searchId, "searchId");
      Objects.requireNonNull(target, "target");
    }
  }

  /** A candidate was inspected; this does not imply a visit or a comparison. */
  record SearchProbed(String searchId, AlgorithmEvent.Reference candidate)
      implements AlgorithmEvent {
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
  record SearchFound(String searchId, AlgorithmEvent.Reference result)
      implements AlgorithmEvent {
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
      implements AlgorithmEvent {
    public SearchCompleted {
      searchId = requireName(searchId, "searchId");
      if (resultCount < 0L) throw new IllegalArgumentException("resultCount must not be negative");
    }
  }

  /** An algorithm-internal memo/cache lookup found a value; no structure read is implied. */
  record CacheHit(String cacheId, String key) implements AlgorithmEvent {
    public CacheHit {
      cacheId = requireName(cacheId, "cacheId");
      key = requireName(key, "key");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.CACHE_HITS, 1L);
    }
  }

  record CacheMiss(String cacheId, String key) implements AlgorithmEvent {
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
  record CacheStored(String cacheId, String key) implements AlgorithmEvent {
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
  record CacheEvicted(String cacheId, String key) implements AlgorithmEvent {
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
  record CandidateAdded(String frontierId, String candidateId, AlgorithmEvent.Reference ref)
      implements AlgorithmEvent {
    @Override public Map<String, Long> metricDeltas() { return Map.of(MetricKeys.FRONTIER_ADDITIONS, 1L); }
    public CandidateAdded {
      frontierId = requireName(frontierId, "frontierId");
      candidateId = requireName(candidateId, "candidateId");
      Objects.requireNonNull(ref, "ref");
    }
  }

  /** Selection does not imply removal from the frontier or a structure read. */
  record CandidateSelected(String frontierId, String candidateId)
      implements AlgorithmEvent {
    @Override public Map<String, Long> metricDeltas() { return Map.of(MetricKeys.FRONTIER_SELECTIONS, 1L); }
    public CandidateSelected {
      frontierId = requireName(frontierId, "frontierId");
      candidateId = requireName(candidateId, "candidateId");
    }
  }

  /** Candidate rejected without mutation of its underlying structure. */
  record CandidateRejected(String frontierId, String candidateId)
      implements AlgorithmEvent {
    @Override public Map<String, Long> metricDeltas() { return Map.of(MetricKeys.FRONTIER_REJECTIONS, 1L); }
    public CandidateRejected {
      frontierId = requireName(frontierId, "frontierId");
      candidateId = requireName(candidateId, "candidateId");
    }
  }

  /** Candidate discarded by a branch-and-bound/backtracking decision. */
  record CandidatePruned(String frontierId, String candidateId)
      implements AlgorithmEvent {
    @Override public Map<String, Long> metricDeltas() { return Map.of(MetricKeys.FRONTIER_PRUNES, 1L); }
    public CandidatePruned {
      frontierId = requireName(frontierId, "frontierId");
      candidateId = requireName(candidateId, "candidateId");
    }
  }

  /** A logical recursive call is entered. parentCallId is null only for a root call. */
  record CallEntered(String callId, String parentCallId, String label)
      implements AlgorithmEvent {
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
      implements AlgorithmEvent {
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
  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
