package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.event.observation.AlgorithmObservationEvent;
import com.majortom.algorithms.core.event.observation.ObservationEvent;
import java.util.List;
import java.util.function.ToIntFunction;

/** Thin construction/publishing helpers for explicit factual algorithm observations. */
public final class Observations {
  private Observations() {}

  /** Publish a custom read-only algorithm event through the existing ordered observation path. */
  public static void algorithm(AlgorithmObservationEvent event) {
    ExecutionEvents.observe(event);
  }

  public static void searchStarted(String searchId, ObservationEvent.Reference target) {
    algorithm(new AlgorithmObservationEvent.SearchStarted(searchId, target));
  }

  public static void searchProbed(String searchId, ObservationEvent.Reference candidate) {
    algorithm(new AlgorithmObservationEvent.SearchProbed(searchId, candidate));
  }

  public static void searchFound(String searchId, ObservationEvent.Reference result) {
    algorithm(new AlgorithmObservationEvent.SearchFound(searchId, result));
  }

  public static void searchCompleted(String searchId, long resultCount) {
    algorithm(new AlgorithmObservationEvent.SearchCompleted(searchId, resultCount));
  }

  public static void cacheHit(String cacheId, String key) {
    algorithm(new AlgorithmObservationEvent.CacheHit(cacheId, key));
  }

  public static void cacheMiss(String cacheId, String key) {
    algorithm(new AlgorithmObservationEvent.CacheMiss(cacheId, key));
  }

  public static void cacheStored(String cacheId, String key) {
    algorithm(new AlgorithmObservationEvent.CacheStored(cacheId, key));
  }

  public static void cacheEvicted(String cacheId, String key) {
    algorithm(new AlgorithmObservationEvent.CacheEvicted(cacheId, key));
  }

  public static void candidateAdded(String frontierId, String candidateId, ObservationEvent.Reference ref) {
    algorithm(new AlgorithmObservationEvent.CandidateAdded(frontierId, candidateId, ref));
  }

  public static void candidateSelected(String frontierId, String candidateId) {
    algorithm(new AlgorithmObservationEvent.CandidateSelected(frontierId, candidateId));
  }

  public static void candidateRejected(String frontierId, String candidateId) {
    algorithm(new AlgorithmObservationEvent.CandidateRejected(frontierId, candidateId));
  }

  public static void candidatePruned(String frontierId, String candidateId) {
    algorithm(new AlgorithmObservationEvent.CandidatePruned(frontierId, candidateId));
  }

  public static void callEntered(String callId, String parentCallId, String label) {
    algorithm(new AlgorithmObservationEvent.CallEntered(callId, parentCallId, label));
  }

  public static void callReturned(String callId, String resultSummary) {
    algorithm(new AlgorithmObservationEvent.CallReturned(callId, resultSummary));
  }

  public static void compared(
      String leftSource, int leftIndex, String rightSource, int rightIndex) {
    ExecutionEvents.observe(
        new ObservationEvent.Compared(new ObservationEvent.IndexRef(leftSource, leftIndex),
            new ObservationEvent.IndexRef(rightSource, rightIndex)));
  }

  public static void compared(String source, int index, Object value) {
    ExecutionEvents.observe(new ObservationEvent.Compared(
        new ObservationEvent.IndexRef(source, index), new ObservationEvent.ValueRef(value)));
  }

  public static void visited(String domain, long id) {
    ExecutionEvents.observe(
        new ObservationEvent.Visited(new ObservationEvent.EntityRef(domain, id)));
  }

  public static void visited(int row, int column) {
    ExecutionEvents.observe(
        new ObservationEvent.Visited(new ObservationEvent.CoordinateRef(row, column)));
  }

  public static void examined(String domain, long fromId, long toId) {
    ExecutionEvents.observe(
        new ObservationEvent.Examined(new ObservationEvent.EntityRef(domain, fromId),
            new ObservationEvent.EntityRef(domain, toId)));
  }

  public static void examined(int fromRow, int fromColumn, int toRow, int toColumn) {
    ExecutionEvents.observe(
        new ObservationEvent.Examined(new ObservationEvent.CoordinateRef(fromRow, fromColumn),
            new ObservationEvent.CoordinateRef(toRow, toColumn)));
  }

  public static void matched(int index, int length) {
    ExecutionEvents.observe(new ObservationEvent.Matched(index, length));
  }

  public static void fallback(int fromIndex, int toIndex) {
    ExecutionEvents.observe(new ObservationEvent.Fallback(fromIndex, toIndex));
  }

  public static void backtracked(String domain, long id) {
    ExecutionEvents.observe(
        new ObservationEvent.Backtracked(new ObservationEvent.EntityRef(domain, id)));
  }

  public static void backtracked(int row, int column) {
    ExecutionEvents.observe(
        new ObservationEvent.Backtracked(new ObservationEvent.CoordinateRef(row, column)));
  }

  public static void pathTraced(String domain, long id) {
    ExecutionEvents.observe(
        new ObservationEvent.PathTraced(new ObservationEvent.EntityRef(domain, id)));
  }

  public static void pathTraced(int row, int column) {
    ExecutionEvents.observe(
        new ObservationEvent.PathTraced(new ObservationEvent.CoordinateRef(row, column)));
  }

  public static void pathFound(List<ObservationEvent.Reference> refs) {
    ExecutionEvents.observe(new ObservationEvent.PathFound(refs));
  }

  public static <T> void pathFound(
      List<T> values, ToIntFunction<? super T> row, ToIntFunction<? super T> column) {
    List<ObservationEvent.Reference> refs =
        values.stream()
            .map(value
                -> (ObservationEvent.Reference) new ObservationEvent.CoordinateRef(
                    row.applyAsInt(value), column.applyAsInt(value)))
            .toList();
    pathFound(refs);
  }
}
