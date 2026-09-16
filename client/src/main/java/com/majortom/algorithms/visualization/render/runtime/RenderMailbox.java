package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderResult;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.ViewportRenderIntent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;

final class RenderMailbox {
    Submission pendingStructure;
    Submission pendingViewport;
    final Deque<Submission> presentationQueue = new ArrayDeque<>();
    boolean inFlight;

    void enqueue(Submission submission) {
        RenderIntent intent = submission.intent();
        if (intent instanceof StructuralRenderIntent<?>) {
            if (pendingStructure != null)
                pendingStructure.future().complete(RenderResult.superseded(intent.sessionId()));
            pendingStructure = submission;
        } else if (intent instanceof ViewportRenderIntent) {
            if (pendingViewport != null)
                pendingViewport.future().complete(RenderResult.superseded(intent.sessionId()));
            pendingViewport = submission;
        } else if (intent instanceof PresentationRenderIntent<?>) {
            presentationQueue.addLast(submission);
        } else {
            throw new IllegalArgumentException("Unsupported render intent: " + intent);
        }
    }

    Submission poll() {
        if (pendingStructure != null) {
            Submission next = pendingStructure;
            pendingStructure = null;
            return next;
        }
        if (!presentationQueue.isEmpty()) return presentationQueue.removeFirst();
        if (pendingViewport != null) {
            Submission next = pendingViewport;
            pendingViewport = null;
            return next;
        }
        return null;
    }

    void cancelPending() {
        if (pendingStructure != null)
            pendingStructure
                    .future()
                    .complete(RenderResult.cancelled(pendingStructure.intent().sessionId()));
        if (pendingViewport != null)
            pendingViewport
                    .future()
                    .complete(RenderResult.cancelled(pendingViewport.intent().sessionId()));
        pendingStructure = null;
        pendingViewport = null;
        while (!presentationQueue.isEmpty()) {
            Submission next = presentationQueue.removeFirst();
            next.future().complete(RenderResult.cancelled(next.intent().sessionId()));
        }
    }

    record Submission(
            RenderIntent intent,
            CompletableFuture<RenderResult> future,
            long modelRevision,
            long geometryRevision) {}
}
