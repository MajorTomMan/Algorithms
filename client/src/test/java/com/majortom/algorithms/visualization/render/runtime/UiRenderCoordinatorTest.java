package com.majortom.algorithms.visualization.render.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.visualization.render.fx.FxExecutor;
import com.majortom.algorithms.visualization.settings.FontSettings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class UiRenderCoordinatorTest {
  @Test
  void mergesFontAndShellRequestsBeforeTheNextTurn() {
    FakeFx fx = new FakeFx();
    Participant participant = new Participant();
    UiRenderCoordinator coordinator = new UiRenderCoordinator(fx, participant);
    CompletionStage<Void> first = coordinator.requestFont(font(18));
    CompletionStage<Void> second = coordinator.requestFont(font(22));
    CompletionStage<Void> shell = coordinator.requestWorkbench();
    fx.runNext();
    assertEquals(List.of("font:22", "css:true", "workbench", "geometry:22"),
        participant.events);
    assertTrue(first.toCompletableFuture().isDone());
    assertTrue(second.toCompletableFuture().isDone());
    assertTrue(shell.toCompletableFuture().isDone());
    assertFalse(fx.hasPending());
  }

  @Test
  void waitsForStructureInvalidationBeforeStartingTheNextBatch() {
    FakeFx fx = new FakeFx();
    Participant participant = new Participant();
    participant.geometry = new CompletableFuture<>();
    UiRenderCoordinator coordinator = new UiRenderCoordinator(fx, participant);
    CompletionStage<Void> font = coordinator.requestFont(font(24));
    fx.runNext();
    CompletionStage<Void> shell = coordinator.requestWorkbench();
    assertEquals(List.of("font:24", "css:true", "workbench", "geometry:24"),
        participant.events);
    assertFalse(font.toCompletableFuture().isDone());
    assertFalse(shell.toCompletableFuture().isDone());
    participant.geometry.complete(null);
    fx.runNext();
    assertEquals(List.of("font:24", "css:true", "workbench", "geometry:24",
        "css:false", "workbench"), participant.events);
    assertTrue(font.toCompletableFuture().isDone());
    assertTrue(shell.toCompletableFuture().isDone());
  }

  @Test
  void preparesMountedNodesBeforeCompletingTheirRenderBarrier() {
    FakeFx fx = new FakeFx();
    Participant participant = new Participant();
    UiRenderCoordinator coordinator = new UiRenderCoordinator(fx, participant);
    CompletionStage<Void> ready = coordinator.requestMountedContent();
    assertFalse(ready.toCompletableFuture().isDone());
    assertTrue(participant.events.isEmpty());
    fx.runNext();
    assertEquals(List.of("css:false", "workbench"), participant.events);
    assertTrue(ready.toCompletableFuture().isDone());
  }

  @Test
  void aMountCannotOvertakeAnInFlightFontChange() {
    FakeFx fx = new FakeFx();
    Participant participant = new Participant();
    participant.geometry = new CompletableFuture<>();
    UiRenderCoordinator coordinator = new UiRenderCoordinator(fx, participant);
    CompletionStage<Void> font = coordinator.requestFont(font(24));
    fx.runNext();
    CompletionStage<Void> mounted = coordinator.requestMountedContent();
    assertFalse(mounted.toCompletableFuture().isDone());
    assertEquals(List.of("font:24", "css:true", "workbench", "geometry:24"),
        participant.events);
    participant.geometry.complete(null);
    fx.runNext();
    assertEquals(List.of("font:24", "css:true", "workbench", "geometry:24",
        "css:false", "workbench"), participant.events);
    assertTrue(font.toCompletableFuture().isDone());
    assertTrue(mounted.toCompletableFuture().isDone());
  }

  private static FontSettings font(double size) {
    return new FontSettings("", "", size, "");
  }

  private static final class Participant implements UiRenderCoordinator.Participant {
    final List<String> events = new ArrayList<>();
    CompletableFuture<Void> geometry = CompletableFuture.completedFuture(null);

    @Override public void applyFont(FontSettings settings) {
      events.add("font:" + (int) settings.size());
    }

    @Override public void prepareStyles(boolean changed) {
      events.add("css:" + changed);
    }

    @Override public void refreshWorkbench() {
      events.add("workbench");
    }

    @Override public CompletionStage<Void> refreshContentGeometry(FontSettings settings) {
      events.add("geometry:" + (int) settings.size());
      return geometry;
    }
  }

  private static final class FakeFx implements FxExecutor {
    private final Queue<Runnable> delayed = new ArrayDeque<>();

    @Override public CompletionStage<Void> execute(Runnable action) {
      try {
        action.run();
        return CompletableFuture.completedFuture(null);
      } catch (Throwable failure) {
        return CompletableFuture.failedFuture(failure);
      }
    }

    @Override public CompletionStage<Void> defer(Runnable action) {
      CompletableFuture<Void> done = new CompletableFuture<>();
      delayed.add(() -> {
        try {
          action.run();
          done.complete(null);
        } catch (Throwable failure) {
          done.completeExceptionally(failure);
        }
      });
      return done;
    }

    @Override public <T> CompletionStage<T> supply(Supplier<T> supplier) {
      try {
        return CompletableFuture.completedFuture(supplier.get());
      } catch (Throwable failure) {
        return CompletableFuture.failedFuture(failure);
      }
    }

    @Override public boolean isFxThread() { return true; }
    boolean hasPending() { return !delayed.isEmpty(); }
    void runNext() { delayed.remove().run(); }
  }
}
