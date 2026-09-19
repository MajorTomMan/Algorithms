package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.fx.FxExecutor;
import com.majortom.algorithms.visualization.settings.FontSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Orders Workbench style preparation, shell layout and structure-style invalidation.
 * It never interprets CSS, calculates geometry or evaluates the rendered result.
 *
 * <p>Requests arriving before the next FX turn are coalesced. Requests arriving while
 * an earlier geometry invalidation is in flight run in the following turn. A
 * completion means the participant's steps completed, not that pixels were inspected.</p>
 */
public final class UiRenderCoordinator {
  public interface Participant {
    /** Existing font service applies preferences to the current root. FX thread only. */
    void applyFont(FontSettings settings);

    /** Prepare current (including newly attached) nodes for CSS measurement. FX thread only. */
    void prepareStyles(boolean fontChanged);

    /** Existing Workbench layout module applies its own layout. FX thread only. */
    void refreshWorkbench();

    /** Invalidate affected structure geometry after Workbench/viewport changes settle. */
    CompletionStage<Void> refreshContentGeometry(FontSettings settings);
  }

  private final FxExecutor fx;
  private final Participant participant;
  private final List<CompletableFuture<Void>> pending = new ArrayList<>();
  private FontSettings pendingFont;
  private boolean pendingWorkbench;
  private boolean scheduled;
  private boolean running;

  public UiRenderCoordinator(FxExecutor fx, Participant participant) {
    this.fx = Objects.requireNonNull(fx, "fx");
    this.participant = Objects.requireNonNull(participant, "participant");
  }

  /** A font change also invalidates the Workbench and the active structure layout. */
  public CompletionStage<Void> requestFont(FontSettings settings) {
    Objects.requireNonNull(settings, "settings");
    return enqueue(settings);
  }

  /** Shell/locale/viewport requests are coalesced without invalidating structure geometry. */
  public CompletionStage<Void> requestWorkbench() {
    return enqueue(null);
  }

  /**
   * A new visualizer is attached synchronously before its controller can publish its first
   * structural intent. Prepare its CSS here, rather than waiting for an unrelated page switch.
   */
  public void prepareMountedContent() {
    if (!fx.isFxThread()) {
      throw new IllegalStateException("Visualizer mounting must occur on the FX thread");
    }
    if (!running && pendingFont != null) {
      flush();
    }
    participant.prepareStyles(false);
    requestWorkbench();
  }

  private CompletionStage<Void> enqueue(FontSettings font) {
    CompletableFuture<Void> result = new CompletableFuture<>();
    fx.execute(() -> {
      if (font != null) pendingFont = font;
      pendingWorkbench = true;
      pending.add(result);
      schedule();
    }).whenComplete((ignored, failure) -> {
      if (failure != null) result.completeExceptionally(failure);
    });
    return result;
  }

  private void schedule() {
    if (running || scheduled || pending.isEmpty()) return;
    scheduled = true;
    fx.defer(this::flush).whenComplete((ignored, failure) -> {
      if (failure != null) fx.execute(() -> failPending(failure));
    });
  }

  private void flush() {
    if (running || pending.isEmpty()) return;
    scheduled = false;
    running = true;
    FontSettings font = pendingFont;
    boolean workbench = pendingWorkbench;
    pendingFont = null;
    pendingWorkbench = false;
    List<CompletableFuture<Void>> batch = new ArrayList<>(pending);
    pending.clear();

    CompletionStage<Void> completion;
    try {
      if (font != null) participant.applyFont(font);
      participant.prepareStyles(font != null);
      if (workbench) participant.refreshWorkbench();
      completion = font == null
          ? CompletableFuture.completedFuture(null)
          : Objects.requireNonNull(participant.refreshContentGeometry(font),
              "refreshContentGeometry must return a completion stage");
    } catch (Throwable failure) {
      finish(batch, failure);
      return;
    }
    completion.whenComplete((ignored, failure) ->
        fx.execute(() -> finish(batch, failure)));
  }

  private void finish(List<CompletableFuture<Void>> batch, Throwable failure) {
    running = false;
    for (CompletableFuture<Void> waiter : batch) {
      if (failure == null) waiter.complete(null);
      else waiter.completeExceptionally(failure);
    }
    schedule();
  }

  private void failPending(Throwable failure) {
    scheduled = false;
    pendingFont = null;
    pendingWorkbench = false;
    List<CompletableFuture<Void>> batch = new ArrayList<>(pending);
    pending.clear();
    for (CompletableFuture<Void> waiter : batch) waiter.completeExceptionally(failure);
  }
}
