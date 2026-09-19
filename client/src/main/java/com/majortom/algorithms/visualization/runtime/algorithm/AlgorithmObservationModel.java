package com.majortom.algorithms.visualization.runtime.algorithm;

import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** JavaFX-free, immutable algorithm-only projection of search and memoization facts. */
public record AlgorithmObservationModel(String runId, long sequence,
    Map<String, Search> searches, String currentSearchId,
    List<CacheEntry> recentCache, Pulse pulse,
    Map<String, Frontier> frontiers, String currentFrontierId, List<CallFrame> callStack) {
  public static final int MAX_RECENT_CACHE = 8;
  public static final int MAX_VISIBLE_CANDIDATES = 8;
  public static final int MAX_VISIBLE_CALLS = 8;

  public AlgorithmObservationModel {
    runId = Objects.requireNonNull(runId, "runId");
    searches = Map.copyOf(Objects.requireNonNull(searches, "searches"));
    recentCache = List.copyOf(Objects.requireNonNull(recentCache, "recentCache"));
    pulse = Objects.requireNonNull(pulse, "pulse");
    frontiers = Map.copyOf(Objects.requireNonNull(frontiers, "frontiers"));
    callStack = List.copyOf(Objects.requireNonNull(callStack, "callStack"));
  }

  public static AlgorithmObservationModel empty() {
    return new AlgorithmObservationModel("", -1L, Map.of(), null, List.of(), Pulse.none(), Map.of(), null, List.of());
  }

  public boolean hasContent() {
    return !searches.isEmpty() || !recentCache.isEmpty() || !frontiers.isEmpty()
        || !callStack.isEmpty() || pulse.kind() != Kind.NONE;
  }

  public Frontier currentFrontier() {
    return currentFrontierId == null ? null : frontiers.get(currentFrontierId);
  }

  public Search currentSearch() {
    return currentSearchId == null ? null : searches.get(currentSearchId);
  }

  /** Reduce one authoritative event. Structural and ordinary observation facts never alter this layer. */
  public static AlgorithmObservationModel apply(AlgorithmObservationModel previous, EventEnvelope envelope) {
    Objects.requireNonNull(previous, "previous");
    Objects.requireNonNull(envelope, "envelope");
    AlgorithmObservationModel state = previous.runId().equals(envelope.runId())
        ? previous : new AlgorithmObservationModel(envelope.runId(), -1L, Map.of(), null, List.of(), Pulse.none(), Map.of(), null, List.of());
    Object fact = envelope.event();
    Map<String, Search> searches = state.searches();
    String currentSearch = state.currentSearchId();
    List<CacheEntry> cache = state.recentCache();
    Map<String, Frontier> frontiers = state.frontiers();
    String currentFrontier = state.currentFrontierId();
    List<CallFrame> callStack = state.callStack();
    Pulse pulse = Pulse.none();
    if (fact instanceof AlgorithmEvent.SearchStarted start) {
      searches = copyWith(searches, start.searchId(),
          new Search(start.searchId(), referenceText(start.target()), "", "", 0, 0, false));
      currentSearch = start.searchId();
      pulse = new Pulse(Kind.SEARCH_STARTED, start.searchId(), referenceText(start.target()));
    } else if (fact instanceof AlgorithmEvent.SearchProbed probe) {
      Search before = searches.getOrDefault(probe.searchId(), Search.unknown(probe.searchId()));
      String candidate = referenceText(probe.candidate());
      searches = copyWith(searches, probe.searchId(), new Search(probe.searchId(), before.target(),
          candidate, before.lastFound(), before.probes() + 1, before.resultCount(), false));
      currentSearch = probe.searchId();
      pulse = new Pulse(Kind.SEARCH_PROBED, probe.searchId(), candidate);
    } else if (fact instanceof AlgorithmEvent.SearchFound found) {
      Search before = searches.getOrDefault(found.searchId(), Search.unknown(found.searchId()));
      String result = referenceText(found.result());
      searches = copyWith(searches, found.searchId(), new Search(found.searchId(), before.target(),
          before.candidate(), result, before.probes(), before.resultCount() + 1, false));
      currentSearch = found.searchId();
      pulse = new Pulse(Kind.SEARCH_FOUND, found.searchId(), result);
    } else if (fact instanceof AlgorithmEvent.SearchCompleted done) {
      Search before = searches.getOrDefault(done.searchId(), Search.unknown(done.searchId()));
      searches = copyWith(searches, done.searchId(), new Search(done.searchId(), before.target(),
          before.candidate(), before.lastFound(), before.probes(), done.resultCount(), true));
      currentSearch = done.searchId();
      pulse = new Pulse(Kind.SEARCH_COMPLETED, done.searchId(), Long.toString(done.resultCount()));
    } else if (fact instanceof AlgorithmEvent.CacheHit hit) {
      cache = updateCache(cache, hit.cacheId(), hit.key(), Kind.CACHE_HIT);
      pulse = new Pulse(Kind.CACHE_HIT, hit.cacheId(), safeText(hit.key()));
    } else if (fact instanceof AlgorithmEvent.CacheMiss miss) {
      cache = updateCache(cache, miss.cacheId(), miss.key(), Kind.CACHE_MISS);
      pulse = new Pulse(Kind.CACHE_MISS, miss.cacheId(), safeText(miss.key()));
    } else if (fact instanceof AlgorithmEvent.CacheStored stored) {
      cache = updateCache(cache, stored.cacheId(), stored.key(), Kind.CACHE_STORED);
      pulse = new Pulse(Kind.CACHE_STORED, stored.cacheId(), safeText(stored.key()));
    } else if (fact instanceof AlgorithmEvent.CacheEvicted evicted) {
      cache = updateCache(cache, evicted.cacheId(), evicted.key(), Kind.CACHE_EVICTED);
      pulse = new Pulse(Kind.CACHE_EVICTED, evicted.cacheId(), safeText(evicted.key()));
    }
    if (fact instanceof AlgorithmEvent.CandidateAdded added) {
      Frontier before = frontiers.getOrDefault(added.frontierId(), Frontier.empty(added.frontierId()));
      Map<String, Candidate> candidates = new LinkedHashMap<>(before.candidates());
      candidates.put(added.candidateId(), new Candidate(added.candidateId(),
          referenceText(added.ref()), CandidateStatus.PENDING));
      frontiers = copyWith(frontiers, added.frontierId(), new Frontier(added.frontierId(), candidates));
      currentFrontier = added.frontierId();
      pulse = new Pulse(Kind.CANDIDATE_ADDED, added.frontierId(), added.candidateId());
    } else if (fact instanceof AlgorithmEvent.CandidateSelected selected) {
      frontiers = changeCandidate(frontiers, selected.frontierId(), selected.candidateId(), CandidateStatus.SELECTED);
      currentFrontier = selected.frontierId();
      pulse = new Pulse(Kind.CANDIDATE_SELECTED, selected.frontierId(), selected.candidateId());
    } else if (fact instanceof AlgorithmEvent.CandidateRejected rejected) {
      frontiers = changeCandidate(frontiers, rejected.frontierId(), rejected.candidateId(), CandidateStatus.REJECTED);
      currentFrontier = rejected.frontierId();
      pulse = new Pulse(Kind.CANDIDATE_REJECTED, rejected.frontierId(), rejected.candidateId());
    } else if (fact instanceof AlgorithmEvent.CandidatePruned pruned) {
      frontiers = changeCandidate(frontiers, pruned.frontierId(), pruned.candidateId(), CandidateStatus.PRUNED);
      currentFrontier = pruned.frontierId();
      pulse = new Pulse(Kind.CANDIDATE_PRUNED, pruned.frontierId(), pruned.candidateId());
    } else if (fact instanceof AlgorithmEvent.CallEntered entered) {
      // A bad parent reference must not corrupt the active call stack during replay.
      String currentId = callStack.isEmpty() ? null : callStack.getLast().id();
      if (Objects.equals(currentId, entered.parentCallId())
          && callStack.stream().noneMatch(frame -> frame.id().equals(entered.callId()))) {
        ArrayList<CallFrame> next = new ArrayList<>(callStack);
        next.add(new CallFrame(entered.callId(), safeText(entered.label())));
        callStack = List.copyOf(next);
        pulse = new Pulse(Kind.CALL_ENTERED, entered.callId(), safeText(entered.label()));
      } else {
        pulse = new Pulse(Kind.CALL_INVALID, entered.callId(), "invalid parent or duplicate call id");
      }
    } else if (fact instanceof AlgorithmEvent.CallReturned returned) {
      if (!callStack.isEmpty() && callStack.getLast().id().equals(returned.callId())) {
        callStack = List.copyOf(callStack.subList(0, callStack.size() - 1));
        pulse = new Pulse(Kind.CALL_RETURNED, returned.callId(), safeText(returned.resultSummary()));
      } else {
        pulse = new Pulse(Kind.CALL_INVALID, returned.callId(), "non-top call return");
      }
    }
    return new AlgorithmObservationModel(state.runId(), envelope.sequence(), searches,
        currentSearch, cache, pulse, frontiers, currentFrontier, callStack);
  }

  private static Map<String, Frontier> changeCandidate(Map<String, Frontier> before,
      String frontierId, String candidateId, CandidateStatus status) {
    Frontier frontier = before.get(frontierId);
    if (frontier == null || !frontier.candidates().containsKey(candidateId)) return before;
    Map<String, Candidate> updated = new LinkedHashMap<>(frontier.candidates());
    Candidate candidate = updated.get(candidateId);
    updated.put(candidateId, new Candidate(candidate.id(), candidate.reference(), status));
    return copyWith(before, frontierId, new Frontier(frontierId, updated));
  }

  public record Frontier(String id, Map<String, Candidate> candidates) {
    public Frontier { candidates = java.util.Collections.unmodifiableMap(
        new LinkedHashMap<>(Objects.requireNonNull(candidates, "candidates"))); }
    public static Frontier empty(String id) { return new Frontier(id, Map.of()); }
    public long count(CandidateStatus status) {
      return candidates.values().stream().filter(c -> c.status() == status).count();
    }
    public List<Candidate> recentCandidates() {
      List<Candidate> all = new ArrayList<>(candidates.values());
      return List.copyOf(all.subList(Math.max(0, all.size() - MAX_VISIBLE_CANDIDATES), all.size()));
    }
  }

  public record Candidate(String id, String reference, CandidateStatus status) {}
  public enum CandidateStatus { PENDING, SELECTED, REJECTED, PRUNED }
  public record CallFrame(String id, String label) {}

  private static <T> Map<String, T> copyWith(Map<String, T> before, String id, T next) {
    Map<String, T> updated = new LinkedHashMap<>(before);
    updated.put(id, next);
    return Map.copyOf(updated);
  }

  /** This is only a bounded list of recent cache facts, not an authoritative cache contents map. */
  private static List<CacheEntry> updateCache(List<CacheEntry> before, String cacheId, String key, Kind kind) {
    List<CacheEntry> updated = new ArrayList<>(MAX_RECENT_CACHE);
    updated.add(new CacheEntry(safeText(cacheId), safeText(key), kind));
    for (CacheEntry entry : before) {
      if (entry.cacheId().equals(safeText(cacheId)) && entry.key().equals(safeText(key))) continue;
      if (updated.size() >= MAX_RECENT_CACHE) break;
      updated.add(entry);
    }
    return List.copyOf(updated);
  }

  static String referenceText(AlgorithmEvent.Reference ref) {
    if (ref instanceof AlgorithmEvent.IndexRef index) return safeText(index.source()) + "[" + index.index() + "]";
    if (ref instanceof AlgorithmEvent.EntityRef entity) return safeText(entity.domain()) + " #" + entity.id();
    if (ref instanceof AlgorithmEvent.CoordinateRef position) return "(" + position.row() + ", " + position.column() + ")";
    if (ref instanceof AlgorithmEvent.ValueRef value) {
      try { return safeText(String.valueOf(value.value())); }
      catch (RuntimeException failure) { return "<value unavailable>"; }
    }
    return "<unknown reference>";
  }

  private static String safeText(String input) {
    if (input == null) return "";
    String text = input.replace('\n', ' ').replace('\r', ' ');
    return text.length() <= 90 ? text : text.substring(0, 87) + "...";
  }

  public record Search(String id, String target, String candidate, String lastFound,
      long probes, long resultCount, boolean completed) {
    public static Search unknown(String id) { return new Search(id, "", "", "", 0, 0, false); }
  }

  public record CacheEntry(String cacheId, String key, Kind lastAction) {}

  public record Pulse(Kind kind, String scope, String detail) {
    public static Pulse none() { return new Pulse(Kind.NONE, "", ""); }
  }

  public enum Kind {
    NONE, SEARCH_STARTED, SEARCH_PROBED, SEARCH_FOUND, SEARCH_COMPLETED,
    CACHE_HIT, CACHE_MISS, CACHE_STORED, CACHE_EVICTED,
    CANDIDATE_ADDED, CANDIDATE_SELECTED, CANDIDATE_REJECTED, CANDIDATE_PRUNED,
    CALL_ENTERED, CALL_RETURNED, CALL_INVALID
  }
}
