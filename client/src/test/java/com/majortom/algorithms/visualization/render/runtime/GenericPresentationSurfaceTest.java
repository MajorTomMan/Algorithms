package com.majortom.algorithms.visualization.render.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.diagnostics.RenderTrace;
import com.majortom.algorithms.visualization.render.fx.FxExecutor;
import com.majortom.algorithms.visualization.render.fx.FxPresentationSurfaceRegistry;
import com.majortom.algorithms.visualization.render.api.PresentationSurface;
import com.majortom.algorithms.visualization.render.fx.RenderSurfaceRegistry;
import com.majortom.algorithms.visualization.render.layout.LayoutEngineRegistry;
import com.majortom.algorithms.visualization.render.presentation.MutablePresentationModelSource;
import com.majortom.algorithms.visualization.render.api.PresentationCursor;
import com.majortom.algorithms.visualization.render.viewport.CameraManager;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GenericPresentationSurfaceTest {
  @Test
  void capturesImmutableModelAndCommitsThroughRenderFramework() throws Exception {
    RenderSessionId surfaceId = RenderSessionId.of("test:stats");
    MutablePresentationModelSource<String> source = new MutablePresentationModelSource<>("first");
    AtomicReference<String> committed = new AtomicReference<>();

    try (DefaultRenderFramework framework = framework()) {
      framework
          .registerPresentationSurface(PresentationSurface.standalone(
              surfaceId,
              source,
              (model, context) -> {
                committed.set(model);
                return CompletableFuture.completedFuture(null);
              }))
          .toCompletableFuture()
          .get();
      framework.activatePresentationSurface(surfaceId).toCompletableFuture().get();
      framework.invalidatePresentationSurface(surfaceId).toCompletableFuture().get();
      assertEquals("first", committed.get());

      source.publish("second");
      framework.invalidatePresentationSurface(surfaceId).toCompletableFuture().get();
      assertEquals("second", committed.get());
      assertEquals(0L, framework.trace().layoutInvocationCount(surfaceId));
    }
  }

  @Test
  void cursorPublicationInvalidatesDependentPresentationSurface() throws Exception {
    RenderSessionId cursorSession = RenderSessionId.of("structure:array");
    RenderSessionId surfaceId = RenderSessionId.of("test:timeline");
    AtomicReference<PresentationCursor> observedCursor = new AtomicReference<>();
    AtomicReference<String> committed = new AtomicReference<>();

    try (DefaultRenderFramework framework = framework()) {
      framework
          .registerPresentationSurface(PresentationSurface.followingCursor(
              surfaceId,
              cursorSession,
              context -> {
                context.cursor().ifPresent(observedCursor::set);
                return context.cursor().map(cursor -> "event-" + cursor.eventSequence()).orElse("none");
              },
              (model, context) -> {
                committed.set(model);
                return CompletableFuture.completedFuture(null);
              }))
          .toCompletableFuture()
          .get();
      framework.activatePresentationSurface(surfaceId).toCompletableFuture().get();

      PresentationCursor cursor = PresentationCursor.atEvent(
          "run-7", 12L, PresentationCursor.Mode.REPLAY);
      framework.publishPresentationCursor(cursorSession, cursor).toCompletableFuture().get();

      long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
      while (!"event-12".equals(committed.get()) && System.nanoTime() < deadline) {
        Thread.sleep(5L);
      }
      assertEquals(cursor, observedCursor.get());
      assertEquals("event-12", committed.get());
      assertTrue(framework.trace().entries(surfaceId).stream()
          .anyMatch(entry -> "PRESENTED".equals(entry.stage())));
      assertEquals(0L, framework.trace().layoutInvocationCount(surfaceId));
    }
  }

  private static DefaultRenderFramework framework() {
    return new DefaultRenderFramework(
        new RenderScheduler(),
        new LayoutExecutor(1),
        new ImmediateFxExecutor(),
        new RenderSurfaceRegistry(),
        new FxPresentationSurfaceRegistry(),
        new LayoutEngineRegistry(),
        new CameraManager(),
        new RenderTrace());
  }

  private static final class ImmediateFxExecutor implements FxExecutor {
    @Override
    public CompletionStage<Void> execute(Runnable action) {
      action.run();
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> defer(Runnable action) {
      action.run();
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T> CompletionStage<T> supply(Supplier<T> supplier) {
      return CompletableFuture.completedFuture(supplier.get());
    }

    @Override
    public boolean isFxThread() {
      return true;
    }
  }
}
