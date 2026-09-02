package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.domain.execution.RunStartedEvent;
import com.majortom.algorithms.core.event.ExecutionEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordingEventSinkTest {

    @Test
    void recordsTheTypedRunnerEventStreamWithoutAnAdapter() {
        Instant timestamp = Instant.parse("2026-08-13T00:00:00Z");
        RecordingEventSink sink = new RecordingEventSink();
        ExecutionRuntime runtime = new ExecutionRuntime(Clock.fixed(timestamp, ZoneOffset.UTC), () -> "run-1");

        ExecutionResult result = runtime.execute("example", sink, () -> {
            ExecutionEvents.emit(new TestEvent("payload"));
            return new TestOutput("payload");
        });
        ExecutionRecording recording = sink.snapshot();

        assertEquals(ExecutionStatus.COMPLETED, result.status());
        assertEquals(ExecutionRecordingState.COMPLETED, recording.state());
        assertEquals(3L, recording.statistics().totalEventCount());
        assertEquals(1L, recording.statistics().domainEventCount());
        assertEquals(timestamp, recording.statistics().startedAt().orElseThrow());
        assertEquals(timestamp, recording.statistics().endedAt().orElseThrow());
        assertEquals(new TestEvent("payload"), recording.events().get(1).event());
    }

    @Test
    void createsImmutablePointInTimeSnapshotsThatCanBeReplayed() {
        Instant startedAt = Instant.parse("2026-08-13T01:00:00Z");
        Instant endedAt = Instant.parse("2026-08-13T01:00:01Z");
        RecordingEventSink sink = new RecordingEventSink();

        assertFalse(sink.hasEvents());
        assertThrows(IllegalStateException.class, sink::snapshot);

        sink.accept(event(0L, startedAt, new RunStartedEvent()));
        sink.accept(event(1L, startedAt.plusMillis(100L), new TestEvent("first")));
        ExecutionRecording runningSnapshot = sink.snapshot();

        sink.accept(event(2L, startedAt.plusMillis(200L), new TestEvent("second")));
        sink.accept(event(3L, endedAt, new RunCompletedEvent()));
        ExecutionRecording completedSnapshot = sink.snapshot();

        assertEquals(ExecutionRecordingState.RUNNING, runningSnapshot.state());
        assertEquals(2, runningSnapshot.events().size());
        assertEquals(ExecutionRecordingState.COMPLETED, completedSnapshot.state());
        assertEquals("run-1", completedSnapshot.runId());
        assertEquals("example", completedSnapshot.operationId());
        assertEquals(4L, completedSnapshot.statistics().totalEventCount());
        assertEquals(2L, completedSnapshot.statistics().domainEventCount());
        assertEquals(2L, completedSnapshot.statistics().lifecycleEventCount());
        assertEquals(startedAt, completedSnapshot.statistics().startedAt().orElseThrow());
        assertEquals(endedAt, completedSnapshot.statistics().endedAt().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> completedSnapshot.events().clear());

        RecordingEventSink replaySink = new RecordingEventSink();
        completedSnapshot.replay(replaySink);
        assertEquals(completedSnapshot, replaySink.snapshot());
    }

    @Test
    void recordsValidationFailureWithoutInventingAStartTimestamp() {
        Instant failedAt = Instant.parse("2026-08-13T02:00:00Z");
        RecordingEventSink sink = new RecordingEventSink();

        sink.accept(event(0L, failedAt, new RunFailedEvent("algorithm.input.invalid", "bad input")));
        ExecutionRecording recording = sink.snapshot();

        assertEquals(ExecutionRecordingState.FAILED, recording.state());
        assertEquals(1L, recording.statistics().totalEventCount());
        assertEquals(0L, recording.statistics().domainEventCount());
        assertTrue(recording.statistics().startedAt().isEmpty());
        assertEquals(failedAt, recording.statistics().endedAt().orElseThrow());
    }

    @Test
    void rejectsInvalidEnvelopesAndLifecycleOrderWithoutMutatingTheRecording() {
        RecordingEventSink sink = new RecordingEventSink();
        Instant timestamp = Instant.parse("2026-08-13T03:00:00Z");

        assertThrows(IllegalArgumentException.class,
                () -> sink.accept(event(0L, timestamp, new TestEvent("before-start"))));
        assertFalse(sink.hasEvents());

        sink.accept(event(0L, timestamp, new RunStartedEvent()));
        assertThrows(IllegalArgumentException.class,
                () -> sink.accept(new EventEnvelope(
                        "other-run", "example", 1L, timestamp, "example", new TestEvent("mixed"))));
        assertThrows(IllegalArgumentException.class,
                () -> sink.accept(event(2L, timestamp, new TestEvent("gap"))));
        assertThrows(IllegalArgumentException.class,
                () -> sink.accept(event(1L, timestamp, new RunStartedEvent())));

        sink.accept(event(1L, timestamp, new TestEvent("valid")));
        sink.accept(event(2L, timestamp, new RunCompletedEvent()));
        assertEquals(List.of(0L, 1L, 2L), sink.snapshot().events().stream()
                .map(EventEnvelope::sequence)
                .toList());
        assertThrows(IllegalArgumentException.class,
                () -> sink.accept(event(3L, timestamp, new TestEvent("after-terminal"))));
        assertEquals(3L, sink.snapshot().statistics().totalEventCount());
    }

    private EventEnvelope event(long sequence, Instant timestamp, ExecutionEvent event) {
        return new EventEnvelope("run-1", "example", sequence, timestamp, "example", event);
    }

    private record TestEvent(String value) implements ExecutionEvent {
    }

    private record TestOutput(String value) {
    }
}
