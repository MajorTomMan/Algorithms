package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.visualization.runtime.Reduction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalAlgorithmExecutionTest {

    @Test
    void replacementRunUsesAnIndependentExecutor() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicReference<String> firstThread = new AtomicReference<>();
        AtomicReference<String> secondThread = new AtomicReference<>();
        ExecutionOperation<TestOutput> slow = () -> {
            firstThread.set(Thread.currentThread().getName());
            firstStarted.countDown();
            awaitIgnoringInterrupt(releaseFirst);
            return new TestOutput("first");
        };
        ExecutionOperation<TestOutput> fast = () -> {
            secondThread.set(Thread.currentThread().getName());
            return new TestOutput("second");
        };

        try (LocalAlgorithmExecution execution = execution(Runnable::run)) {
            ExecutionSession first = execution.start("slow", slow, reducer("", EventEnvelope::runId), state -> { });
            assertTrue(firstStarted.await(1L, TimeUnit.SECONDS));

            ExecutionSession second = execution.start("fast", fast, reducer("", EventEnvelope::runId), state -> { });
            ExecutionResult secondResult = second.presentationCompletion().get(1L, TimeUnit.SECONDS);

            assertEquals(ExecutionStatus.COMPLETED, secondResult.status());
            assertFalse(first.presentationCompletion().isDone());
            assertFalse(firstThread.get().equals(secondThread.get()));
            releaseFirst.countDown();
            assertEquals(ExecutionStatus.CANCELLED, first.presentationCompletion().get(1L, TimeUnit.SECONDS).status());
        } finally {
            releaseFirst.countDown();
        }
    }

    @Test
    void cancellationWakesExecutionPausedAtACheckpoint() throws Exception {
        CountDownLatch beforeCheckpoint = new CountDownLatch(1);
        CountDownLatch enterCheckpoint = new CountDownLatch(1);
        ExecutionOperation<TestOutput> operation = () -> {
            beforeCheckpoint.countDown();
            enterCheckpoint.await();
            ExecutionEvents.checkpoint();
            return new TestOutput("value");
        };

        try (LocalAlgorithmExecution execution = execution(Runnable::run)) {
            ExecutionSession session = execution.start(
                    "pause", operation, reducer("", event -> event.event().getClass().getSimpleName()), state -> { });
            assertTrue(beforeCheckpoint.await(1L, TimeUnit.SECONDS));
            session.pauseExecution();
            enterCheckpoint.countDown();
            Thread.sleep(50L);

            session.cancel();

            ExecutionResult result = session.presentationCompletion().get(1L, TimeUnit.SECONDS);
            assertEquals(ExecutionStatus.CANCELLED, result.status());
            assertTrue(session.events().stream().anyMatch(event -> event.event() instanceof RunCancelledEvent));
        } finally {
            enterCheckpoint.countDown();
        }
    }

    @Test
    void delayedCallbacksFromAStaleGenerationCannotProject() throws Exception {
        List<Runnable> uiTasks = new CopyOnWriteArrayList<>();
        List<String> projectedRunIds = new ArrayList<>();

        try (LocalAlgorithmExecution execution = execution(uiTasks::add)) {
            ExecutionSession first = execution.start(
                    "first", echo("first"), reducer("", EventEnvelope::runId), projectedRunIds::add);
            assertTrue(waitForTask(uiTasks));
            Runnable staleCallback = uiTasks.remove(0);

            ExecutionSession second = execution.start(
                    "second", echo("second"), reducer("", EventEnvelope::runId), projectedRunIds::add);
            assertEquals(ExecutionStatus.COMPLETED, first.presentationCompletion().get(1L, TimeUnit.SECONDS).status());
            assertTrue(waitForTask(uiTasks));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1L);
            while (!second.presentationCompletion().isDone() && System.nanoTime() < deadline) {
                drain(uiTasks);
                Thread.sleep(5L);
            }
            drain(uiTasks);
            assertEquals(ExecutionStatus.COMPLETED, second.presentationCompletion().get(1L, TimeUnit.SECONDS).status());

            staleCallback.run();

            assertFalse(projectedRunIds.contains(first.events().get(0).runId()));
            assertTrue(projectedRunIds.stream().allMatch(runId -> runId.equals(second.events().get(0).runId())));
            assertFalse(projectedRunIds.isEmpty());
        }
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

    @Test
    void closeCancelsCurrentRunAndRejectsNewRuns() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutionOperation<TestOutput> operation = () -> {
            started.countDown();
            release.await();
            ExecutionEvents.checkpoint();
            return new TestOutput("value");
        };
        LocalAlgorithmExecution execution = execution(Runnable::run);
        ExecutionSession session = execution.start("close", operation, reducer("", EventEnvelope::runId), state -> { });
        assertTrue(started.await(1L, TimeUnit.SECONDS));

        execution.close();
        release.countDown();

        assertEquals(ExecutionStatus.CANCELLED, session.presentationCompletion().get(1L, TimeUnit.SECONDS).status());
        assertTrue(session.isClosed());
        assertThrows(IllegalStateException.class, () -> execution.start(
                "echo", echo("value"), reducer("", event -> ""), state -> { }));
    }

    @Test
    void dispatcherFailureDoesNotLoseAuthoritativeEventsOrFailTheRun() throws Exception {
        LocalAlgorithmExecution execution = execution(runnable -> {
            throw new IllegalStateException("JavaFX is unavailable");
        });
        try (execution) {
            ExecutionSession session = execution.start(
                    "echo", echo("value"), reducer("", event -> ""), state -> { });

            ExecutionResult result = session.presentationCompletion().get(1L, TimeUnit.SECONDS);

            assertEquals(ExecutionStatus.COMPLETED, result.status());
            assertEquals(2, session.events().size());
            assertTrue(session.dispatcherFailure().isPresent());
        }
    }

    @Test
    void eventLimitProducesABoundedAuthoritativeFailureRecord() throws Exception {
        ExecutionOperation<TestOutput> noisy = () -> {
            for (int index = 0; index < 20; index++) {
                ExecutionEvents.emit(new TestEvent(index));
            }
            return new TestOutput("value");
        };
        try (LocalAlgorithmExecution execution = new LocalAlgorithmExecution(
                new ExecutionRuntime(), Runnable::run, 8)) {
            ExecutionSession session = execution.start(
                    "noisy", noisy, reducer("", event -> ""), state -> { });

            ExecutionResult result = session.presentationCompletion().get(1L, TimeUnit.SECONDS);

            assertEquals(ExecutionStatus.FAILED, result.status());
            assertEquals(8, session.events().size());
            assertEquals("client.execution.event-limit-exceeded", result.failure().orElseThrow().code());
            assertTrue(session.events().getLast().event() instanceof RunFailedEvent);
        }
    }

    @Test
    void playbackDelayDoesNotExtendRuntimeDuration() throws Exception {
        List<Runnable> uiTasks = new CopyOnWriteArrayList<>();
        ExecutionOperation<TestOutput> operation = () -> {
            ExecutionEvents.emit(new TestEvent(1));
            return new TestOutput("value");
        };
        try (LocalAlgorithmExecution execution = execution(uiTasks::add)) {
            ExecutionSession session = execution.start(
                    "paced", operation, reducer(-1L, EventEnvelope::sequence), state -> { }, () -> 10_000L);

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1L);
            while (session.totalDuration().isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(5L);
            }

            assertTrue(session.totalDuration().isPresent());
            assertTrue(session.totalDuration().orElseThrow().toMillis() < 1_000L);
            assertTrue(session.runtimeCompletion().isDone());
            assertFalse(session.presentationCompletion().isDone());
            session.closeObserver();
            assertEquals(ExecutionStatus.COMPLETED, session.presentationCompletion().get(1L, TimeUnit.SECONDS).status());
            assertTrue(session.events().stream().anyMatch(event -> event.event() instanceof com.majortom.algorithms.core.domain.execution.RunCompletedEvent));
        }
    }

    @Test
    void fatalWorkerErrorStillCompletesTheSessionExceptionally() {
        ExecutionOperation<TestOutput> fatal = () -> {
            throw new AssertionError("fatal");
        };
        try (LocalAlgorithmExecution execution = execution(Runnable::run)) {
            ExecutionSession session = execution.start(
                    "fatal", fatal, reducer("", event -> ""), state -> { });

            assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> session.presentationCompletion().get(1L, TimeUnit.SECONDS));
            assertTrue(session.presentationCompletion().isCompletedExceptionally());
        }
    }

    private LocalAlgorithmExecution execution(java.util.function.Consumer<Runnable> dispatcher) {
        return new LocalAlgorithmExecution(new ExecutionRuntime(), dispatcher);
    }

    private <S> EventReducer<S> reducer(S initialState, Function<EventEnvelope, S> mapping) {
        return new EventReducer<>() {
            @Override
            public S initialState() {
                return initialState;
            }

            @Override
            public Reduction<S> reduce(S previousState, EventEnvelope event) {
                return Reduction.changed(mapping.apply(event), EventImportance.STATE_CHANGE, true);
            }
        };
    }

    private ExecutionOperation<TestOutput> echo(String value) {
        return () -> new TestOutput(value);
    }

    private void drain(List<Runnable> uiTasks) {
        while (!uiTasks.isEmpty()) {
            uiTasks.remove(0).run();
        }
    }

    private void awaitIgnoringInterrupt(CountDownLatch latch) {
        boolean released = false;
        while (!released) {
            try {
                released = latch.await(50L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.interrupted();
            }
        }
    }

    private record TestOutput(String value) {
    }

    private record TestEvent(int value) implements ExecutionEvent {
    }
}
