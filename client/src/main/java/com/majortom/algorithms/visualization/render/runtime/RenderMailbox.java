package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.PresentationSurfaceRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderResult;
import com.majortom.algorithms.visualization.render.api.RenderStatus;
import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.StructuralChange;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.ViewportRenderIntent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;

/** Per-session latest-wins mailbox. MODEL and derived GEOMETRY have separate lanes. */
final class RenderMailbox {
  Submission pendingModel;
  Submission pendingGeometry;
  Submission pendingViewport;
  Submission pendingSurfacePresentation;
  Submission pendingRefresh;
  private static final int DEFAULT_MAX_ORDERED_PRESENTATIONS = 4_096;
  private final int maxOrderedPresentations;
  final Deque<Submission> presentationQueue = new ArrayDeque<>();

  RenderMailbox() { this(DEFAULT_MAX_ORDERED_PRESENTATIONS); }

  RenderMailbox(int maxOrderedPresentations) {
    if (maxOrderedPresentations < 1) throw new IllegalArgumentException("maxOrderedPresentations must be positive");
    this.maxOrderedPresentations = maxOrderedPresentations;
  }
  boolean inFlight;

  void enqueue(Submission submission) {
    RenderIntent intent = submission.intent();
    if (intent instanceof StructuralRenderIntent<?> structural) {
      if (structural.change() == StructuralChange.MODEL) {
        supersede(pendingModel, intent);
        pendingModel = submission;
        // Geometry captured from an older factual model is no longer meaningful.
        supersede(pendingGeometry, intent);
        pendingGeometry = null;
      } else {
        supersede(pendingGeometry, intent);
        pendingGeometry = submission;
      }
    } else if (intent instanceof ViewportRenderIntent) {
      supersede(pendingViewport, intent);
      pendingViewport = submission;
    } else if (intent instanceof PresentationSurfaceRenderIntent) {
      supersede(pendingSurfacePresentation, intent);
      pendingSurfacePresentation = submission;
    } else if (intent instanceof PresentationRenderIntent<?> presentation) {
      if (presentation.coalescible()) {
        supersede(pendingRefresh, intent);
        pendingRefresh = submission;
      } else if (presentationQueue.size() < maxOrderedPresentations) {
        presentationQueue.addLast(submission);
      } else {
        // Ordered animation frames cannot be merged without changing playback semantics.
        // Report overload instead of retaining an unbounded number of snapshots.
        submission.future().complete(new RenderResult(RenderStatus.FAILED, intent.sessionId(),
            submission.modelRevision(), false, BoundsSnapshot.empty(),
            new IllegalStateException("Ordered presentation queue capacity exceeded: "
                + maxOrderedPresentations)));
      }
    } else {
      throw new IllegalArgumentException("Unsupported render intent: " + intent);
    }
  }

  Submission poll() {
    if (pendingModel != null) {
      Submission next = pendingModel;
      pendingModel = null;
      return next;
    }
    if (pendingGeometry != null) {
      Submission next = pendingGeometry;
      pendingGeometry = null;
      return next;
    }
    // Viewport commands remain responsive while ordered animations are playing.
    if (pendingViewport != null) {
      Submission next = pendingViewport;
      pendingViewport = null;
      return next;
    }
    if (!presentationQueue.isEmpty()) return presentationQueue.removeFirst();
    if (pendingRefresh != null) {
      Submission next = pendingRefresh;
      pendingRefresh = null;
      return next;
    }
    if (pendingSurfacePresentation != null) {
      Submission next = pendingSurfacePresentation;
      pendingSurfacePresentation = null;
      return next;
    }
    return null;
  }

  void cancelPending() {
    cancel(pendingModel);
    cancel(pendingGeometry);
    cancel(pendingViewport);
    cancel(pendingSurfacePresentation);
    cancel(pendingRefresh);
    pendingModel = null;
    pendingGeometry = null;
    pendingViewport = null;
    pendingSurfacePresentation = null;
    pendingRefresh = null;
    while (!presentationQueue.isEmpty()) cancel(presentationQueue.removeFirst());
  }

  private static void supersede(Submission pending, RenderIntent replacement) {
    if (pending != null) {
      pending.future().complete(RenderResult.superseded(replacement.sessionId()));
    }
  }

  private static void cancel(Submission submission) {
    if (submission != null) {
      submission.future().complete(RenderResult.cancelled(submission.intent().sessionId()));
    }
  }

  record Submission(RenderIntent intent, CompletableFuture<RenderResult> future, long modelRevision,
      long geometryRevision) {}
}
