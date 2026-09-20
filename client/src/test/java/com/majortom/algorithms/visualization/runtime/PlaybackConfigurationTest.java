package com.majortom.algorithms.visualization.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PlaybackConfigurationTest {
    @Test
    void eventBudgetAndLiveQueueShareTheSameDefaultSource() {
        assertEquals(ExecutionLimits.DEFAULT_MAXIMUM_EVENT_COUNT,
                LocalAlgorithmExecution.DEFAULT_MAXIMUM_EVENT_COUNT);
        assertEquals(ExecutionLimits.DEFAULT_LIVE_QUEUE_CAPACITY, JavaFxEventSink.DEFAULT_CAPACITY);
    }

    @Test
    void playbackSpeedBoundsAreSharedWithTheAnimationPlayer() {
        assertEquals(PlaybackTiming.MIN_SPEED, PlaybackTiming.clampSpeed(0.01d));
        assertEquals(PlaybackTiming.MAX_SPEED, PlaybackTiming.clampSpeed(50.0d));
        assertEquals(8.0d, PlaybackTiming.clampSpeed(8.0d));
    }
}
