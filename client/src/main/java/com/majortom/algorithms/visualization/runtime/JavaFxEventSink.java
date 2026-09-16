package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.EventSink;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import com.majortom.algorithms.visualization.render.runtime.RenderRuntime;
import com.majortom.algorithms.visualization.render.timing.RenderTimer;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Ordered live-presentation queue for authoritative Runtime events; timing is delegated to
 * RenderClock.
 */
public final class JavaFxEventSink implements EventSink, AutoCloseable {
  static final int DEFAULT_CAPACITY = 200_000;

  private final Consumer<Runnable> dispatcher;
  private final Consumer<EventEnvelope> consumer;
  private final LongSupplier delayMillisSupplier;
  private final int capacity;
  private final RenderTimer timer;
  private final Object lock = new Object();
  private final Deque<EventEnvelope> pendingEvents = new ArrayDeque<>();

  private boolean dispatchInFlight;
  private boolean paused;
  private int stepPermits;
  private boolean closed;
  private RuntimeException dispatcherFailure;
  private RuntimeException observerFailure;
  private long observerFailureCount;
  private CompletableFuture<Void> drained = CompletableFuture.completedFuture(null);

  public JavaFxEventSink(Consumer<EventEnvelope> consumer) {
    this(FxDispatch::defer, consumer, DEFAULT_CAPACITY, () -> 0L);
  }

  JavaFxEventSink(Consumer<Runnable> dispatcher, Consumer<EventEnvelope> consumer) {
    this(dispatcher, consumer, DEFAULT_CAPACITY, () -> 0L);
  }

  JavaFxEventSink(Consumer<Runnable> dispatcher, Consumer<EventEnvelope> consumer, int capacity,
      int ignoredBatchSize) {
    this(dispatcher, consumer, capacity, () -> 0L);
  }

  JavaFxEventSink(Consumer<Runnable> dispatcher, Consumer<EventEnvelope> consumer, int capacity,
      LongSupplier delayMillisSupplier) {
    this(dispatcher, consumer, capacity, delayMillisSupplier, RenderRuntime.clock());
  }

  JavaFxEventSink(Consumer<Runnable> dispatcher, Consumer<EventEnvelope> consumer, int capacity,
      LongSupplier delayMillisSupplier, RenderTimer timer) {
    this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    this.consumer = Objects.requireNonNull(consumer, "consumer");
    this.delayMillisSupplier = Objects.requireNonNull(delayMillisSupplier, "delayMillisSupplier");
    this.timer = Objects.requireNonNull(timer, "timer");
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be positive");
    }
    this.capacity = capacity;
  }

  @Override
  public void accept(EventEnvelope event) {
    Objects.requireNonNull(event, "event");
    synchronized (lock) {
      if (closed || dispatcherFailure != null || observerFailure != null) {
        return;
      }
      if (pendingEvents.size() >= capacity) {
        observerFailure =
            new IllegalStateException("Live playback queue capacity exceeded: " + capacity);
        observerFailureCount++;
        pendingEvents.clear();
        completeDrainedIfIdle();
        return;
      }
      if (pendingEvents.isEmpty() && !dispatchInFlight) {
        drained = new CompletableFuture<>();
      }
      pendingEvents.addLast(event);
      scheduleNextLocked(0L);
    }
  }

  public void pause() {
    synchronized (lock) {
      paused = true;
      stepPermits = 0;
    }
  }

  public void resume() {
    synchronized (lock) {
      paused = false;
      stepPermits = 0;
      scheduleNextLocked(0L);
    }
  }

  public void step() {
    synchronized (lock) {
      if (!paused || closed) {
        return;
      }
      stepPermits++;
      scheduleNextLocked(0L);
    }
  }

  public CompletableFuture<Void> drained() {
    synchronized (lock) {
      return drained;
    }
  }

  public Optional<RuntimeException> dispatcherFailure() {
    synchronized (lock) {
      return Optional.ofNullable(dispatcherFailure);
    }
  }

  public Optional<RuntimeException> observerFailure() {
    synchronized (lock) {
      return Optional.ofNullable(observerFailure);
    }
  }

  public long observerFailureCount() {
    synchronized (lock) {
      return observerFailureCount;
    }
  }

  int pendingEventCount() {
    synchronized (lock) {
      return pendingEvents.size();
    }
  }

  @Override
  public void close() {
    synchronized (lock) {
      if (closed) {
        return;
      }
      closed = true;
      pendingEvents.clear();
      dispatchInFlight = false;
      if (!drained.isDone()) {
        drained.complete(null);
      }
    }
  }

  private void scheduleNextLocked(long delayMillis) {
    if (closed || dispatcherFailure != null || observerFailure != null || dispatchInFlight
        || pendingEvents.isEmpty()) {
      completeDrainedIfIdle();
      return;
    }
    if (paused && stepPermits == 0) {
      return;
    }
    EventEnvelope next = pendingEvents.peekFirst();
    long delay;
    if (next != null && next.event() instanceof ExecutionLifecycleEvent) {
      delay = 0L;
    } else {
      delay = Math.max(0L, delayMillis);
    }
    dispatchInFlight = true;
    try {
      timer.schedule(Duration.ofMillis(delay), this::dispatchNext);
    } catch (RuntimeException exception) {
      dispatcherFailure = exception;
      dispatchInFlight = false;
      pendingEvents.clear();
      completeDrainedIfIdle();
    }
  }

  private void dispatchNext() {
    EventEnvelope event;
    synchronized (lock) {
      if (closed || dispatcherFailure != null || observerFailure != null) {
        dispatchInFlight = false;
        completeDrainedIfIdle();
        return;
      }
      if (paused && stepPermits == 0) {
        dispatchInFlight = false;
        return;
      }
      event = pendingEvents.pollFirst();
      if (paused && stepPermits > 0) {
        stepPermits--;
      }
      if (event == null) {
        dispatchInFlight = false;
        completeDrainedIfIdle();
        return;
      }
    }
    try {
      dispatcher.accept(() -> consumeAndContinue(event));
    } catch (RuntimeException exception) {
      synchronized (lock) {
        dispatcherFailure = exception;
        dispatchInFlight = false;
        pendingEvents.clear();
        completeDrainedIfIdle();
      }
    }
  }

  private void consumeAndContinue(EventEnvelope event) {
    synchronized (lock) {
      if (closed) {
        dispatchInFlight = false;
        completeDrainedIfIdle();
        return;
      }
    }
    try {
      consumer.accept(event);
    } catch (RuntimeException exception) {
      synchronized (lock) {
        observerFailure = exception;
        observerFailureCount++;
        pendingEvents.clear();
      }
    } finally {
      synchronized (lock) {
        dispatchInFlight = false;
        if (closed || dispatcherFailure != null || observerFailure != null) {
          completeDrainedIfIdle();
          return;
        }
        long delay;
        if (event.event() instanceof ExecutionLifecycleEvent) {
          delay = 0L;
        } else {
          delay = Math.max(0L, delayMillisSupplier.getAsLong());
        }
        scheduleNextLocked(delay);
      }
    }
  }

  private void completeDrainedIfIdle() {
    if (!dispatchInFlight && pendingEvents.isEmpty() && !drained.isDone()) {
      drained.complete(null);
    }
  }
}
