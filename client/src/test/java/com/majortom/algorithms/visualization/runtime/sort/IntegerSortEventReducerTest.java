package com.majortom.algorithms.visualization.runtime.sort;

import com.majortom.algorithms.core.domain.execution.RunStartedEvent;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.InMemoryEventSink;
import com.majortom.algorithms.visualization.runtime.ReductionCursor;
import com.majortom.algorithms.library.sort.insertion.IntegerInsertionSort;
import com.majortom.algorithms.library.sort.event.SortInitializedEvent;
import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegerSortEventReducerTest {

    @Test
    void reducesACompleteTimelineWithoutJson() {
        InMemoryEventSink sink = new InMemoryEventSink();
        IntegerInsertionSort algorithm = new IntegerInsertionSort();
        new ExecutionRuntime().execute(
                "insertion-sort", sink, () -> algorithm.sort(new IntegerSortInput(List.of(4, 3, 1, 2))));
        ReductionCursor<IntegerSortViewState> cursor = new ReductionCursor<>(new IntegerSortEventReducer());
        sink.events().forEach(cursor::accept);
        IntegerSortViewState state = cursor.state();
        assertEquals(List.of(1, 2, 3, 4), state.values());
        assertTrue(state.completed());
        assertEquals(-1, state.comparedIndex());
    }

    @Test
    void cursorRejectsMixedRunsAndSequenceGaps() {
        ReductionCursor<IntegerSortViewState> cursor = new ReductionCursor<>(new IntegerSortEventReducer());
        cursor.accept(new EventEnvelope("run-1", "insertion-sort", 0L, Instant.EPOCH, "insertion-sort",
                new RunStartedEvent()));
        assertThrows(IllegalArgumentException.class, () -> cursor.accept(new EventEnvelope(
                "run-2", "insertion-sort", 1L, Instant.EPOCH, "insertion-sort",
                new SortInitializedEvent(List.of(2, 1)))));
        assertThrows(IllegalArgumentException.class, () -> cursor.accept(new EventEnvelope(
                "run-1", "insertion-sort", 2L, Instant.EPOCH, "insertion-sort",
                new SortInitializedEvent(List.of(2, 1)))));
    }
}
