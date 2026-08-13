package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.library.sort.event.SortInitializedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxEventSinkTest {

    @Test
    void dispatchesWithoutStartingTheJavaFxToolkit() {
        List<Runnable> scheduled = new ArrayList<>();
        List<ExecutionEvent> consumed = new ArrayList<>();
        JavaFxEventSink sink = new JavaFxEventSink(scheduled::add, consumed::add);
        ExecutionEvent event = new ExecutionEvent(
                "run",
                "insertion-sort",
                0,
                Instant.EPOCH,
                new SortInitializedEvent(List.of(2, 1)));

        sink.accept(event);

        assertEquals(1, scheduled.size());
        assertEquals(0, consumed.size());
        scheduled.get(0).run();
        assertEquals(List.of(event), consumed);
    }

    @Test
    void boundsPendingEventsAndReleasesProducerAsTheUiDrains() throws Exception {
        List<Runnable> scheduled = new ArrayList<>();
        List<ExecutionEvent> consumed = new ArrayList<>();
        JavaFxEventSink sink = new JavaFxEventSink(scheduled::add, consumed::add, 2, 1);
        ExecutionEvent first = event(0L);
        ExecutionEvent second = event(1L);
        ExecutionEvent third = event(2L);
        sink.accept(first);
        sink.accept(second);

        CountDownLatch accepting = new CountDownLatch(1);
        AtomicBoolean accepted = new AtomicBoolean();
        Thread producer = new Thread(() -> {
            accepting.countDown();
            sink.accept(third);
            accepted.set(true);
        });
        producer.start();

        assertTrue(accepting.await(1L, TimeUnit.SECONDS));
        assertFalse(waitUntil(accepted, 100L));
        assertEquals(2, sink.pendingEventCount());

        scheduled.remove(0).run();
        producer.join(1_000L);
        assertTrue(accepted.get());
        while (!scheduled.isEmpty()) {
            scheduled.remove(0).run();
        }
        assertEquals(List.of(first, second, third), consumed);
    }

    @Test
    void dispatcherFailureIsolatedAndRecorded() {
        JavaFxEventSink sink = new JavaFxEventSink(
                runnable -> {
                    throw new IllegalStateException("toolkit stopped");
                },
                event -> {
                    throw new AssertionError("observer must not run");
                });

        sink.accept(event(0L));
        sink.accept(event(1L));

        assertTrue(sink.dispatcherFailure().isPresent());
        assertEquals(0, sink.pendingEventCount());
    }

    @Test
    void closeReleasesAProducerWaitingForQueueSpace() throws Exception {
        List<Runnable> scheduled = new ArrayList<>();
        JavaFxEventSink sink = new JavaFxEventSink(scheduled::add, event -> {
        }, 1, 1);
        sink.accept(event(0L));
        AtomicBoolean returned = new AtomicBoolean();
        Thread producer = new Thread(() -> {
            sink.accept(event(1L));
            returned.set(true);
        });
        producer.start();

        assertFalse(waitUntil(returned, 100L));
        sink.close();
        producer.join(1_000L);

        assertTrue(returned.get());
        assertEquals(0, sink.pendingEventCount());
    }

    private ExecutionEvent event(long sequence) {
        return new ExecutionEvent(
                "run",
                "insertion-sort",
                sequence,
                Instant.EPOCH,
                new SortInitializedEvent(List.of(2, 1)));
    }

    private boolean waitUntil(AtomicBoolean value, long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline) {
            if (value.get()) {
                return true;
            }
            Thread.sleep(5L);
        }
        return value.get();
    }
}
