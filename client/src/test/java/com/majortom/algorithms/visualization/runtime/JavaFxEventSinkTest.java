package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.library.sort.event.SortInitializedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxEventSinkTest {

    @Test
    void dispatchesWithoutStartingTheJavaFxToolkit() throws Exception {
        List<Runnable> scheduled = new ArrayList<>();
        List<EventEnvelope> consumed = new ArrayList<>();
        JavaFxEventSink sink = new JavaFxEventSink(scheduled::add, consumed::add);
        EventEnvelope event = event(0L);

        sink.accept(event);

        assertTrue(waitUntil(() -> !scheduled.isEmpty(), 1_000L));
        assertEquals(0, consumed.size());
        scheduled.remove(0).run();
        assertEquals(List.of(event), consumed);
        sink.close();
    }

    @Test
    void queueCapacityNeverBackpressuresTheProducer() throws Exception {
        JavaFxEventSink sink = new JavaFxEventSink(runnable -> { }, event -> { }, 2, 1);
        sink.pause();
        sink.accept(event(0L));
        sink.accept(event(1L));
        AtomicBoolean returned = new AtomicBoolean();
        Thread producer = new Thread(() -> {
            sink.accept(event(2L));
            returned.set(true);
        });
        producer.start();
        producer.join(1_000L);

        assertTrue(returned.get());
        assertTrue(sink.observerFailure().isPresent());
        sink.close();
    }

    @Test
    void dispatcherFailureIsolatedAndRecorded() throws Exception {
        JavaFxEventSink sink = new JavaFxEventSink(runnable -> {
            throw new IllegalStateException("toolkit stopped");
        }, event -> {
            throw new AssertionError("observer must not run");
        });

        sink.accept(event(0L));

        assertTrue(waitUntil(() -> sink.dispatcherFailure().isPresent(), 1_000L));
        assertEquals(0, sink.pendingEventCount());
        sink.close();
    }

    @Test
    void closeClearsQueuedPlaybackWithoutWaitingForAProducer() throws Exception {
        JavaFxEventSink sink = new JavaFxEventSink(runnable -> { }, event -> { }, 2, 1);
        sink.pause();
        sink.accept(event(0L));
        sink.accept(event(1L));

        long startedAt = System.nanoTime();
        sink.close();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertTrue(elapsedMillis < 500L);
        assertEquals(0, sink.pendingEventCount());
        assertTrue(sink.drained().isDone());
    }

    private EventEnvelope event(long sequence) {
        return new EventEnvelope("run", "insertion-sort", sequence, Instant.EPOCH, "insertion-sort",
                new SortInitializedEvent(List.of(2, 1)));
    }

    private boolean waitUntil(java.util.function.BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(5L);
        }
        return condition.getAsBoolean();
    }
}
