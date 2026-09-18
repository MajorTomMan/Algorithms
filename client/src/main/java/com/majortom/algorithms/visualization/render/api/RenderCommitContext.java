package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;
import java.util.Optional;

/** Immutable transaction metadata visible to the FX commit phase. */
public record RenderCommitContext(
    long transactionId,
    RenderSessionId sessionId,
    long generation,
    long modelRevision,
    long layoutRevision,
    long presentationRevision,
    Optional<StructuralChange> structuralChange,
    boolean initialFrame,
    PresentationProgressSink presentationProgress) {
  public RenderCommitContext {
    Objects.requireNonNull(sessionId, "sessionId");
    structuralChange = Objects.requireNonNull(structuralChange, "structuralChange");
    presentationProgress = Objects.requireNonNull(presentationProgress, "presentationProgress");
  }

  public static RenderCommitContext structural(
      long transactionId,
      RenderSessionId sessionId,
      long generation,
      long modelRevision,
      long layoutRevision,
      long presentationRevision,
      StructuralChange change,
      boolean initialFrame,
      PresentationProgressSink presentationProgress) {
    return new RenderCommitContext(
        transactionId,
        sessionId,
        generation,
        modelRevision,
        layoutRevision,
        presentationRevision,
        Optional.of(Objects.requireNonNull(change, "change")),
        initialFrame,
        Objects.requireNonNull(presentationProgress, "presentationProgress"));
  }

  public static RenderCommitContext presentation(
      long transactionId,
      RenderSessionId sessionId,
      long generation,
      long modelRevision,
      long layoutRevision,
      long presentationRevision) {
    return new RenderCommitContext(
        transactionId,
        sessionId,
        generation,
        modelRevision,
        layoutRevision,
        presentationRevision,
        Optional.empty(),
        false,
        PresentationProgressSink.NONE);
  }

  public boolean modelChange() {
    return structuralChange.orElse(null) == StructuralChange.MODEL;
  }

  public boolean geometryRefresh() {
    return structuralChange.orElse(null) == StructuralChange.GEOMETRY;
  }

  public boolean presentationOnly() {
    return structuralChange.isEmpty();
  }
}
