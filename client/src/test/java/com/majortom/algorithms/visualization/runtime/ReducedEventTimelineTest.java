package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.Reduction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReducedEventTimelineTest {

    @Test
    void forwardAndBackwardSeekUseAStatelessReducer() {
        ReducedEventTimeline<Integer> timeline = new ReducedEventTimeline<>(
                List.of(event(0L, 1), event(1L, 2), event(2L, 3)),
                new SummingReducer());

        assertEquals(1, timeline.seek(0));
        assertEquals(6, timeline.seek(2));
        assertEquals(3, timeline.seek(1));
        assertEquals(6, timeline.seek(2));
    }

    private EventEnvelope event(long sequence, int value) {
        return new EventEnvelope("run", "algorithm", sequence, Instant.EPOCH, "algorithm", new ValueEvent(value));
    }

    private record ValueEvent(int value) implements ExecutionEvent {
    }

    private static final class SummingReducer implements EventReducer<Integer> {

        @Override
        public Integer initialState() {
            return 0;
        }

        @Override
        public Reduction<Integer> reduce(Integer previousState, EventEnvelope event) {
            ValueEvent valueEvent = (ValueEvent) event.event();
            int state = previousState + valueEvent.value();
            return Reduction.changed(state, EventImportance.STATE_CHANGE, true);
        }
    }
}
