package com.majortom.algorithms.library.sort.insertion;

import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.core.runtime.InMemoryEventSink;
import com.majortom.algorithms.library.sort.event.SortComparedEvent;
import com.majortom.algorithms.library.sort.event.SortCompletedEvent;
import com.majortom.algorithms.library.sort.event.SortInitializedEvent;
import com.majortom.algorithms.library.structure.event.ArrayStructureEvent;
import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import com.majortom.algorithms.library.sort.model.IntegerSortOutput;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegerInsertionSortTest {

    @Test
    void sortsAndEmitsAReplayableStronglyTypedTimeline() {
        InMemoryEventSink sink = new InMemoryEventSink();
        IntegerInsertionSort algorithm = new IntegerInsertionSort();
        ExecutionResult result = new ExecutionRuntime().execute(
                "insertion-sort", sink, () -> algorithm.sort(new IntegerSortInput(List.of(5, 2, 4, 2, 1))));

        assertEquals(ExecutionStatus.COMPLETED, result.status());
        IntegerSortOutput output = (IntegerSortOutput) result.output().orElseThrow();
        assertEquals(List.of(1, 2, 2, 4, 5), output.values());
        assertEquals(output.values(), replay(sink.events()));
        assertTrue(sink.events().stream().anyMatch(event -> event.event() instanceof SortComparedEvent));
        assertTrue(sink.events().stream().anyMatch(event -> event.event() instanceof ArrayStructureEvent.Updated));
    }

    @Test
    void inputAndOutputDefensivelyCopyTheirLists() {
        List<Integer> mutable = new ArrayList<>(List.of(3, 1));
        IntegerSortInput input = new IntegerSortInput(mutable);
        mutable.set(0, 0);

        assertEquals(List.of(3, 1), input.values());
        assertTrue(input.values().getClass() != mutable.getClass());
    }

    private List<Integer> replay(List<EventEnvelope> events) {
        List<Integer> values = new ArrayList<>();
        for (EventEnvelope event : events) {
            if (event.event() instanceof SortInitializedEvent initialized) {
                values = new ArrayList<>(initialized.values());
            }
            if (event.event() instanceof ArrayStructureEvent.Updated written) {
                values.set(written.index(), (Integer) written.value());
            }
            if (event.event() instanceof SortCompletedEvent completed) {
                assertEquals(completed.values(), values);
            }
        }
        return values;
    }
}
