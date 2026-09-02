package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.Reduction;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaybackControllerTest {

    @Test
    void playbackPauseAndSpeedAreIndependentControllerState() throws Exception {
        List<Runnable> uiTasks = new CopyOnWriteArrayList<>();
        List<Long> projected = new ArrayList<>();
        PlaybackController<Long> playback = new PlaybackController<>(
                sequenceReducer(),
                projected::add,
                uiTasks::add,
                Duration.ofMillis(200L));
        try (playback) {
            playback.load(List.of(event(0L), event(1L)));
            playback.setSpeed(4.0d);
            assertEquals(4.0d, playback.speed());

            playback.play();
            assertTrue(waitForTask(uiTasks));
            playback.pause();
            uiTasks.remove(0).run();

            assertFalse(playback.isPlaying());
            assertTrue(projected.isEmpty());

            playback.play();
            assertTrue(waitForTask(uiTasks));
            uiTasks.remove(0).run();
            assertEquals(List.of(0L), projected);
            assertTrue(waitForTask(uiTasks));
            uiTasks.remove(0).run();
            assertEquals(List.of(0L, 1L), projected);
            assertFalse(playback.isPlaying());
        }
    }

    @Test
    void closeStopsPlaybackAndRejectsFurtherCommands() {
        PlaybackController<Long> playback = new PlaybackController<>(
                sequenceReducer(),
                state -> {
                },
                Runnable::run,
                Duration.ofMillis(10L));
        playback.close();

        assertThrows(IllegalStateException.class, playback::play);
        assertThrows(IllegalStateException.class, () -> playback.load(List.of(event(0L))));
    }

    private boolean waitForTask(List<Runnable> uiTasks) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1L);
        while (System.nanoTime() < deadline) {
            if (!uiTasks.isEmpty()) {
                return true;
            }
            Thread.sleep(5L);
        }
        return !uiTasks.isEmpty();
    }

    private EventEnvelope event(long sequence) {
        return new EventEnvelope(
                "run",
                "algorithm",
                sequence,
                Instant.EPOCH,
                "algorithm",
                new PlaybackStep());
    }

    private record PlaybackStep() implements ExecutionEvent {
    }

    private EventReducer<Long> sequenceReducer() {
        return new EventReducer<>() {
            @Override
            public Long initialState() {
                return -1L;
            }

            @Override
            public Reduction<Long> reduce(Long previousState, EventEnvelope event) {
                return Reduction.changed(event.sequence(), EventImportance.STATE_CHANGE, true);
            }
        };
    }
}
