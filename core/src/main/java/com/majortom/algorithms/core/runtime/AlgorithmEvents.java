package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import java.util.List;
import java.util.function.ToIntFunction;

/** Algorithm-facing publisher: one event, one runtime checkpoint, one ordered timeline entry. */
public final class AlgorithmEvents {
  private AlgorithmEvents() {}

  /** Publish any general, structure-oriented, or custom algorithm event through the unified timeline. */
  public static void emit(AlgorithmEvent event) {
    ExecutionEvents.algorithm(event);
  }

  public static void searchStarted(String searchId, AlgorithmEvent.Reference target) {
    emit(new AlgorithmEvent.SearchStarted(searchId, target));
  }

  public static void searchProbed(String searchId, AlgorithmEvent.Reference candidate) {
    emit(new AlgorithmEvent.SearchProbed(searchId, candidate));
  }

  public static void searchFound(String searchId, AlgorithmEvent.Reference result) {
    emit(new AlgorithmEvent.SearchFound(searchId, result));
  }

  public static void searchCompleted(String searchId, long resultCount) {
    emit(new AlgorithmEvent.SearchCompleted(searchId, resultCount));
  }

  public static void cacheHit(String cacheId, String key) {
    emit(new AlgorithmEvent.CacheHit(cacheId, key));
  }

  public static void cacheMiss(String cacheId, String key) {
    emit(new AlgorithmEvent.CacheMiss(cacheId, key));
  }

  public static void cacheStored(String cacheId, String key) {
    emit(new AlgorithmEvent.CacheStored(cacheId, key));
  }

  public static void cacheEvicted(String cacheId, String key) {
    emit(new AlgorithmEvent.CacheEvicted(cacheId, key));
  }

  public static void candidateAdded(String frontierId, String candidateId, AlgorithmEvent.Reference ref) {
    emit(new AlgorithmEvent.CandidateAdded(frontierId, candidateId, ref));
  }

  public static void candidateSelected(String frontierId, String candidateId) {
    emit(new AlgorithmEvent.CandidateSelected(frontierId, candidateId));
  }

  public static void candidateRejected(String frontierId, String candidateId) {
    emit(new AlgorithmEvent.CandidateRejected(frontierId, candidateId));
  }

  public static void candidatePruned(String frontierId, String candidateId) {
    emit(new AlgorithmEvent.CandidatePruned(frontierId, candidateId));
  }

  public static void callEntered(String callId, String parentCallId, String label) {
    emit(new AlgorithmEvent.CallEntered(callId, parentCallId, label));
  }

  public static void callReturned(String callId, String resultSummary) {
    emit(new AlgorithmEvent.CallReturned(callId, resultSummary));
  }

  public static void compared(
      String leftSource, int leftIndex, String rightSource, int rightIndex) {
    ExecutionEvents.algorithm(
        new AlgorithmEvent.Compared(new AlgorithmEvent.IndexRef(leftSource, leftIndex),
            new AlgorithmEvent.IndexRef(rightSource, rightIndex)));
  }

  public static void compared(String source, int index, Object value) {
    ExecutionEvents.algorithm(new AlgorithmEvent.Compared(
        new AlgorithmEvent.IndexRef(source, index), new AlgorithmEvent.ValueRef(value)));
  }

  public static void visited(String domain, long id) {
    ExecutionEvents.algorithm(
        new AlgorithmEvent.Visited(new AlgorithmEvent.EntityRef(domain, id)));
  }

  public static void visited(int row, int column) {
    ExecutionEvents.algorithm(
        new AlgorithmEvent.Visited(new AlgorithmEvent.CoordinateRef(row, column)));
  }

  public static void examined(String domain, long fromId, long toId) {
    ExecutionEvents.algorithm(
        new AlgorithmEvent.Examined(new AlgorithmEvent.EntityRef(domain, fromId),
            new AlgorithmEvent.EntityRef(domain, toId)));
  }

  public static void examined(int fromRow, int fromColumn, int toRow, int toColumn) {
    ExecutionEvents.algorithm(
        new AlgorithmEvent.Examined(new AlgorithmEvent.CoordinateRef(fromRow, fromColumn),
            new AlgorithmEvent.CoordinateRef(toRow, toColumn)));
  }

  public static void matched(int index, int length) {
    ExecutionEvents.algorithm(new AlgorithmEvent.Matched(index, length));
  }

  public static void fallback(int fromIndex, int toIndex) {
    ExecutionEvents.algorithm(new AlgorithmEvent.Fallback(fromIndex, toIndex));
  }

  public static void backtracked(String domain, long id) {
    ExecutionEvents.algorithm(
        new AlgorithmEvent.Backtracked(new AlgorithmEvent.EntityRef(domain, id)));
  }

  public static void backtracked(int row, int column) {
    ExecutionEvents.algorithm(
        new AlgorithmEvent.Backtracked(new AlgorithmEvent.CoordinateRef(row, column)));
  }

  public static void pathTraced(String domain, long id) {
    ExecutionEvents.algorithm(
        new AlgorithmEvent.PathTraced(new AlgorithmEvent.EntityRef(domain, id)));
  }

  public static void pathTraced(int row, int column) {
    ExecutionEvents.algorithm(
        new AlgorithmEvent.PathTraced(new AlgorithmEvent.CoordinateRef(row, column)));
  }

  public static void pathFound(List<AlgorithmEvent.Reference> refs) {
    ExecutionEvents.algorithm(new AlgorithmEvent.PathFound(refs));
  }

  public static <T> void pathFound(
      List<T> values, ToIntFunction<? super T> row, ToIntFunction<? super T> column) {
    List<AlgorithmEvent.Reference> refs =
        values.stream()
            .map(value
                -> (AlgorithmEvent.Reference) new AlgorithmEvent.CoordinateRef(
                    row.applyAsInt(value), column.applyAsInt(value)))
            .toList();
    pathFound(refs);
  }
}
