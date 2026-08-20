package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import javafx.application.Platform;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Playback scheduler over reducer-defined visible frames. */
public final class PlaybackController<S> implements AutoCloseable {

    private final EventReducer<S> reducer;
    private final Consumer<S> stateConsumer;
    private final Consumer<Runnable> dispatcher;
    private final ScheduledExecutorService scheduler;
    private final long baseFrameDelayMillis;
    private final Object lock = new Object();

    private ReducedEventTimeline<S> timeline;
    private int currentIndex = -1;
    private long generation;
    private double speed = 1.0d;
    private boolean playing;
    private boolean closed;
    private ScheduledFuture<?> scheduledFrame;
    private RuntimeException failure;
    private long playbackElapsedNanos;
    private long playbackStartedAtNanos;

    public PlaybackController(EventReducer<S> reducer, Consumer<S> stateConsumer) {
        this(reducer, stateConsumer, Platform::runLater, Duration.ofMillis(100L));
    }

    PlaybackController(
            EventReducer<S> reducer,
            Consumer<S> stateConsumer,
            Consumer<Runnable> dispatcher,
            Duration baseFrameDelay) {
        this.reducer = Objects.requireNonNull(reducer, "reducer");
        this.stateConsumer = Objects.requireNonNull(stateConsumer, "stateConsumer");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        Objects.requireNonNull(baseFrameDelay, "baseFrameDelay");
        if (baseFrameDelay.isZero() || baseFrameDelay.isNegative()) {
            throw new IllegalArgumentException("baseFrameDelay must be positive");
        }
        baseFrameDelayMillis = Math.max(1L, baseFrameDelay.toMillis());
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "algorithm-playback");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void load(List<ExecutionEvent> events) {
        Objects.requireNonNull(events, "events");
        ReducedEventTimeline<S> loadedTimeline = new ReducedEventTimeline<>(events, reducer);
        synchronized (lock) {
            requireOpen();
            stopScheduledFrame();
            timeline = loadedTimeline;
            currentIndex = -1;
            playing = false;
            failure = null;
            playbackElapsedNanos = 0L;
            playbackStartedAtNanos = 0L;
            generation++;
        }
    }

    public void play() {
        synchronized (lock) {
            requireOpen();
            if (playing || !hasNextFrame()) {
                return;
            }
            playing = true;
            playbackStartedAtNanos = System.nanoTime();
            generation++;
            scheduleFrame(generation, 0L);
        }
    }

    public void pause() {
        synchronized (lock) {
            requireOpen();
            pauseLocked();
        }
    }

    /** Applies the next visible frame immediately while playback remains paused. */
    public boolean stepForward() {
        int targetIndex;
        long expectedGeneration;
        synchronized (lock) {
            requireOpen();
            pauseLocked();
            if (!hasNextFrame()) {
                return false;
            }
            targetIndex = currentIndex + 1;
            expectedGeneration = generation;
        }
        return applyFrame(expectedGeneration, targetIndex);
    }

    /** Applies the preceding visible frame immediately while playback remains paused. */
    public boolean stepBackward() {
        int targetIndex;
        long expectedGeneration;
        synchronized (lock) {
            requireOpen();
            pauseLocked();
            if (timeline == null || currentIndex <= 0) {
                return false;
            }
            targetIndex = currentIndex - 1;
            expectedGeneration = generation;
        }
        return applyFrame(expectedGeneration, targetIndex);
    }

    /** Seeks to one visible frame and leaves playback paused. */
    public S seek(int frameIndex) {
        S state;
        synchronized (lock) {
            requireOpen();
            pauseLocked();
            ReducedEventTimeline<S> loadedTimeline = requireTimeline();
            state = loadedTimeline.seek(frameIndex);
            currentIndex = frameIndex;
        }
        consumeState(state);
        return state;
    }

    /** Rewinds to the state before the first event without emitting a visible frame. */
    public void restart() {
        synchronized (lock) {
            requireOpen();
            pauseLocked();
            if (timeline != null) {
                timeline.restart();
            }
            currentIndex = -1;
            failure = null;
        }
    }

    public void setSpeed(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier <= 0.0d) {
            throw new IllegalArgumentException("multiplier must be finite and positive");
        }
        synchronized (lock) {
            requireOpen();
            speed = multiplier;
            if (playing) {
                generation++;
                stopScheduledFrame();
                scheduleFrame(generation, frameDelayMillis());
            }
        }
    }

    public double speed() {
        synchronized (lock) {
            return speed;
        }
    }

    public boolean isPlaying() {
        synchronized (lock) {
            return playing;
        }
    }

    public int currentIndex() {
        synchronized (lock) {
            return currentIndex;
        }
    }

    public int nextIndex() {
        synchronized (lock) {
            return currentIndex + 1;
        }
    }

    public int frameCount() {
        synchronized (lock) {
            if (timeline == null) {
                return 0;
            }
            return timeline.size();
        }
    }

    /**
     * Returns time spent actively scheduling replay frames since the last load.
     * Pauses, seeks, and time spent before the first play are excluded.
     */
    public Duration playbackDuration() {
        synchronized (lock) {
            long elapsedNanos = playbackElapsedNanos;
            if (playing) {
                elapsedNanos = Math.addExact(elapsedNanos, elapsedPlaybackNanosLocked());
            }
            return Duration.ofNanos(elapsedNanos);
        }
    }

    public Optional<RuntimeException> failure() {
        synchronized (lock) {
            return Optional.ofNullable(failure);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            if (playing) {
                accumulatePlaybackDurationLocked();
            }
            closed = true;
            playing = false;
            generation++;
            stopScheduledFrame();
        }
        scheduler.shutdownNow();
    }

    private void scheduleFrame(long expectedGeneration, long delayMillis) {
        scheduledFrame = scheduler.schedule(
                () -> dispatchFrame(expectedGeneration),
                delayMillis,
                TimeUnit.MILLISECONDS);
    }

    private void dispatchFrame(long expectedGeneration) {
        try {
            dispatcher.accept(() -> advanceFrame(expectedGeneration));
        } catch (RuntimeException exception) {
            fail(expectedGeneration, exception);
        }
    }

    private void advanceFrame(long expectedGeneration) {
        int targetIndex;
        synchronized (lock) {
            if (closed || !playing || generation != expectedGeneration) {
                return;
            }
            if (!hasNextFrame()) {
                accumulatePlaybackDurationLocked();
                playing = false;
                return;
            }
            targetIndex = currentIndex + 1;
        }

        if (!applyFrame(expectedGeneration, targetIndex)) {
            return;
        }

        synchronized (lock) {
            if (closed || !playing || generation != expectedGeneration) {
                return;
            }
            if (!hasNextFrame()) {
                accumulatePlaybackDurationLocked();
                playing = false;
                return;
            }
            scheduleFrame(expectedGeneration, frameDelayMillis());
        }
    }

    private boolean applyFrame(long expectedGeneration, int targetIndex) {
        try {
            S state;
            synchronized (lock) {
                if (closed || generation != expectedGeneration) {
                    return false;
                }
                state = requireTimeline().seek(targetIndex);
                currentIndex = targetIndex;
            }
            consumeState(state);
            return true;
        } catch (RuntimeException exception) {
            fail(expectedGeneration, exception);
            return false;
        }
    }

    private void consumeState(S state) {
        stateConsumer.accept(state);
    }

    private void fail(long expectedGeneration, RuntimeException exception) {
        synchronized (lock) {
            if (generation != expectedGeneration) {
                return;
            }
            failure = exception;
            if (playing) {
                accumulatePlaybackDurationLocked();
            }
            playing = false;
            generation++;
            stopScheduledFrame();
        }
    }

    private void pauseLocked() {
        if (!playing) {
            return;
        }
        accumulatePlaybackDurationLocked();
        playing = false;
        generation++;
        stopScheduledFrame();
    }

    private boolean hasNextFrame() {
        return timeline != null && currentIndex + 1 < timeline.size();
    }

    private ReducedEventTimeline<S> requireTimeline() {
        if (timeline == null) {
            throw new IllegalStateException("No execution timeline is loaded");
        }
        return timeline;
    }

    private long frameDelayMillis() {
        return Math.max(1L, Math.round(baseFrameDelayMillis / speed));
    }

    private void stopScheduledFrame() {
        if (scheduledFrame != null) {
            scheduledFrame.cancel(false);
            scheduledFrame = null;
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Playback controller is closed");
        }
    }

    private void accumulatePlaybackDurationLocked() {
        playbackElapsedNanos = Math.addExact(playbackElapsedNanos, elapsedPlaybackNanosLocked());
        playbackStartedAtNanos = 0L;
    }

    private long elapsedPlaybackNanosLocked() {
        if (playbackStartedAtNanos == 0L) {
            return 0L;
        }
        long elapsedNanos = System.nanoTime() - playbackStartedAtNanos;
        if (elapsedNanos < 0L) {
            return 0L;
        }
        return elapsedNanos;
    }
}
