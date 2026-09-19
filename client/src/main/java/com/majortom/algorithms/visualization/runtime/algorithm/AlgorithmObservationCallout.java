package com.majortom.algorithms.visualization.runtime.algorithm;

import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.util.Objects;

/** Immutable presentation cue for exactly one authoritative timeline event; never a new event. */
public record AlgorithmObservationCallout(String runId, int cursorIndex, long sequence,
    String titleKey, String titleFallback, String category, String subject, String detail) {

  public AlgorithmObservationCallout {
    Objects.requireNonNull(runId, "runId");
    Objects.requireNonNull(titleKey, "titleKey");
    Objects.requireNonNull(titleFallback, "titleFallback");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(detail, "detail");
  }

  public static AlgorithmObservationCallout empty() {
    return new AlgorithmObservationCallout("", -1, -1L, "", "", "", "", "");
  }

  public static AlgorithmObservationCallout hidden(EventEnvelope current, int cursorIndex) {
    return current == null ? empty() : new AlgorithmObservationCallout(
        current.runId(), cursorIndex, current.sequence(), "", "", "", "", "");
  }

  public boolean visible() { return !titleKey.isEmpty() || !titleFallback.isEmpty(); }

  /** Observe only the current event. Earlier observations must not linger over a structure step. */
  public static AlgorithmObservationCallout at(EventEnvelope current,
      AlgorithmObservationModel model, int cursorIndex) {
    if (current == null || cursorIndex < 0 || !(current.event() instanceof AlgorithmEvent event)) {
      return hidden(current, cursorIndex);
    }
    Objects.requireNonNull(model, "model");
    String category = "generic";
    String subject = "";
    String detail = "";
    String name = event.getClass().getSimpleName();
    if (event instanceof AlgorithmEvent.SearchStarted e) {
      category = "search"; subject = reference(e.target()); detail = e.searchId();
    } else if (event instanceof AlgorithmEvent.SearchProbed e) {
      category = "search"; subject = reference(e.candidate()); detail = e.searchId();
    } else if (event instanceof AlgorithmEvent.SearchFound e) {
      category = "search"; subject = reference(e.result()); detail = e.searchId();
    } else if (event instanceof AlgorithmEvent.SearchCompleted e) {
      category = "search"; subject = e.searchId(); detail = Long.toString(e.resultCount());
    } else if (event instanceof AlgorithmEvent.CacheHit e) {
      category = "cache"; subject = e.key(); detail = e.cacheId();
    } else if (event instanceof AlgorithmEvent.CacheMiss e) {
      category = "cache"; subject = e.key(); detail = e.cacheId();
    } else if (event instanceof AlgorithmEvent.CacheStored e) {
      category = "cache"; subject = e.key(); detail = e.cacheId();
    } else if (event instanceof AlgorithmEvent.CacheEvicted e) {
      category = "cache"; subject = e.key(); detail = e.cacheId();
    } else if (event instanceof AlgorithmEvent.CandidateAdded e) {
      category = "frontier"; subject = candidate(e.frontierId(), e.candidateId(), model);
      detail = e.frontierId();
    } else if (event instanceof AlgorithmEvent.CandidateSelected e) {
      category = "frontier"; subject = candidate(e.frontierId(), e.candidateId(), model);
      detail = e.frontierId();
    } else if (event instanceof AlgorithmEvent.CandidateRejected e) {
      category = "frontier"; subject = candidate(e.frontierId(), e.candidateId(), model);
      detail = e.frontierId();
    } else if (event instanceof AlgorithmEvent.CandidatePruned e) {
      category = "pruned"; subject = candidate(e.frontierId(), e.candidateId(), model);
      detail = e.frontierId();
    } else if (event instanceof AlgorithmEvent.CallEntered e) {
      category = "call"; subject = e.label(); detail = e.callId();
    } else if (event instanceof AlgorithmEvent.CallReturned e) {
      category = "call"; subject = e.callId(); detail = e.resultSummary();
    } else if (event instanceof AlgorithmEvent.Visited e) {
      subject = reference(e.ref());
    } else if (event instanceof AlgorithmEvent.Examined e) {
      subject = reference(e.fromRef()) + " → " + reference(e.toRef());
    } else if (event instanceof AlgorithmEvent.Compared e) {
      subject = reference(e.leftRef()) + " ↔ " + reference(e.rightRef());
    } else if (event instanceof AlgorithmEvent.Backtracked e) {
      subject = reference(e.ref());
    } else if (event instanceof AlgorithmEvent.PathTraced e) {
      subject = reference(e.ref());
    } else if (event instanceof AlgorithmEvent.Matched e) {
      subject = Integer.toString(e.index()); detail = Integer.toString(e.length());
    } else if (event instanceof AlgorithmEvent.Fallback e) {
      subject = e.fromIndex() + " → " + e.toIndex();
    } else if (event instanceof AlgorithmEvent.PathFound e) {
      subject = Integer.toString(e.refs().size());
    } else if (event instanceof AlgorithmEvent.Targeted targeted) {
      subject = reference(targeted.target());
    } else {
      // Custom algorithm events are visible without requiring edits to this presenter.
      // Avoid traversing arbitrary user-defined object graphs or guessing a pruning reason.
      subject = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
    }
    boolean known = event.getClass().getEnclosingClass() == AlgorithmEvent.class
        || event.getClass().getEnclosingClass() == AlgorithmEvent.class;
    String titleKey = known ? "label.algorithm.observation." + camelToSnake(name) : "";
    String fallback = known ? "" : name.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
    return new AlgorithmObservationCallout(current.runId(), cursorIndex, current.sequence(),
        titleKey, fallback, category, bounded(subject), bounded(detail));
  }

  private static String candidate(String frontierId, String id, AlgorithmObservationModel model) {
    AlgorithmObservationModel.Frontier frontier = model.frontiers().get(frontierId);
    AlgorithmObservationModel.Candidate candidate = frontier == null ? null : frontier.candidates().get(id);
    return candidate == null ? id : id + " · " + candidate.reference();
  }

  private static String reference(AlgorithmEvent.Reference ref) {
    return AlgorithmObservationModel.referenceText(ref);
  }

  private static String camelToSnake(String value) {
    return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
  }

  private static String bounded(String text) {
    String singleLine = Objects.toString(text, "").replace('\n', ' ').replace('\r', ' ');
    return singleLine.length() <= 140 ? singleLine : singleLine.substring(0, 137) + "...";
  }
}
