package com.majortom.algorithms.telemetry.memory.runtime;

import com.majortom.algorithms.telemetry.analysis.TelemetryAnalysisSession;
import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetrySessionState;
import com.majortom.algorithms.telemetry.memory.analysis.MemoryAllocationAnalysis;
import com.majortom.algorithms.telemetry.runtime.TelemetrySession;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** One memory telemetry execution lifetime. It owns measurement state, never UI or rendering. */
public final class MemoryTelemetryRun {
  private final MemoryTelemetryService owner;
  private final TelemetrySession session;
  private final TelemetryAnalysisSession analysisSession;
  private final CompletableFuture<MemoryAllocationAnalysis> analysisCompletion = new CompletableFuture<>();
  private boolean finished;

  MemoryTelemetryRun(
      MemoryTelemetryService owner, TelemetrySession session, TelemetryAnalysisSession analysisSession) {
    this.owner = Objects.requireNonNull(owner, "owner");
    this.session = Objects.requireNonNull(session, "session");
    this.analysisSession = analysisSession;
  }

  public TelemetryProfile snapshot() {
    return session.snapshot();
  }

  public CompletionStage<MemoryAllocationAnalysis> analysisCompletion() {
    return analysisCompletion;
  }

  public synchronized boolean finished() {
    return finished;
  }

  public void complete() {
    finish(TelemetrySessionState.COMPLETED);
  }

  public void fail() {
    finish(TelemetrySessionState.FAILED);
  }

  public void cancel() {
    finish(TelemetrySessionState.CANCELLED);
  }

  private synchronized void finish(TelemetrySessionState terminalState) {
    if (finished) return;
    finished = true;
    if (analysisSession != null) analysisSession.markExecutionEnd();
    switch (terminalState) {
      case COMPLETED -> session.complete();
      case FAILED -> session.fail();
      case CANCELLED -> session.cancel();
      default -> throw new IllegalArgumentException("Not a terminal execution state: " + terminalState);
    }
    TelemetryProfile profile = session.snapshot();
    owner.record(profile);
    if (analysisSession == null) {
      analysisCompletion.complete(null);
      return;
    }
    analysisSession.close();
    analysisSession.completion().whenComplete((result, error) -> {
      if (error != null) {
        analysisCompletion.completeExceptionally(error);
        return;
      }
      if (result instanceof MemoryAllocationAnalysis memory) {
        owner.recordAnalysis(memory);
        analysisCompletion.complete(memory);
      } else {
        analysisCompletion.complete(null);
      }
    });
  }
}
