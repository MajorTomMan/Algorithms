package com.majortom.algorithms.library.sort.insertion;

import com.majortom.algorithms.core.runtime.DefaultAlgorithmRunner;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.core.runtime.InMemoryEventSink;
import com.majortom.algorithms.library.catalog.ProviderCatalog;
import com.majortom.algorithms.library.sort.event.SortComparedEvent;
import com.majortom.algorithms.library.sort.event.SortCompletedEvent;
import com.majortom.algorithms.library.sort.event.SortInitializedEvent;
import com.majortom.algorithms.library.sort.event.SortWrittenEvent;
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
        ExecutionResult result = new DefaultAlgorithmRunner().run(
                ProviderCatalog.production().require("insertion-sort").invoker(),
                new IntegerSortInput(List.of(5, 2, 4, 2, 1)),
                sink);

        assertEquals(ExecutionStatus.COMPLETED, result.status());
        IntegerSortOutput output = (IntegerSortOutput) result.output().orElseThrow();
        assertEquals(List.of(1, 2, 2, 4, 5), output.values());
        assertEquals(output.values(), replay(sink.events()));
        assertTrue(sink.events().stream().anyMatch(event -> event.payload() instanceof SortComparedEvent));
        assertTrue(sink.events().stream().anyMatch(event -> event.payload() instanceof SortWrittenEvent));
    }

    @Test
    void inputAndOutputDefensivelyCopyTheirLists() {
        List<Integer> mutable = new ArrayList<>(List.of(3, 1));
        IntegerSortInput input = new IntegerSortInput(mutable);
        mutable.set(0, 0);

        assertEquals(List.of(3, 1), input.values());
        assertTrue(input.values().getClass() != mutable.getClass());
    }

    private List<Integer> replay(List<ExecutionEvent> events) {
        List<Integer> values = new ArrayList<>();
        for (ExecutionEvent event : events) {
            if (event.payload() instanceof SortInitializedEvent initialized) {
                values = new ArrayList<>(initialized.values());
            }
            if (event.payload() instanceof SortWrittenEvent written) {
                values.set(written.index(), written.value());
            }
            if (event.payload() instanceof SortCompletedEvent completed) {
                assertEquals(completed.values(), values);
            }
        }
        return values;
    }
}
