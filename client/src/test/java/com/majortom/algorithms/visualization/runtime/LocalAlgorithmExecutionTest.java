package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.api.Algorithm;
import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmEvent;
import com.majortom.algorithms.core.api.AlgorithmMetadata;
import com.majortom.algorithms.core.api.AlgorithmOutput;
import com.majortom.algorithms.core.api.AlgorithmProvider;
import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.runtime.DefaultAlgorithmRunner;
import com.majortom.algorithms.core.runtime.EventImportance;
import com.majortom.algorithms.core.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.core.runtime.Reduction;
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
        Algorithm<TestInput, TestOutput> slow = (input, context) -> {
            firstThread.set(Thread.currentThread().getName());
            firstStarted.countDown();
            awaitIgnoringInterrupt(releaseFirst);
            return new TestOutput(input.value());
        };
        Algorithm<TestInput, TestOutput> fast = (input, context) -> {
            secondThread.set(Thread.currentThread().getName());
            return new TestOutput(input.value());
        };

        try (LocalAlgorithmExecution execution = execution(Runnable::run)) {
            ExecutionSession first = execution.start(
                    provider("slow", slow).invoker(),
                    new TestInput("first"),
                    reducer("", ExecutionEvent::runId),
                    state -> {
                    });
            assertTrue(firstStarted.await(1L, TimeUnit.SECONDS));

            ExecutionSession second = execution.start(
                    provider("fast", fast).invoker(),
                    new TestInput("second"),
                    reducer("", ExecutionEvent::runId),
                    state -> {
                    });
            ExecutionResult secondResult = second.completion().get(1L, TimeUnit.SECONDS);

            assertEquals(ExecutionStatus.COMPLETED, secondResult.status());
            assertFalse(first.completion().isDone());
            assertFalse(firstThread.get().equals(secondThread.get()));
            releaseFirst.countDown();
            assertEquals(ExecutionStatus.CANCELLED, first.completion().get(1L, TimeUnit.SECONDS).status());
        } finally {
            releaseFirst.countDown();
        }
    }

    @Test
    void cancellationWakesExecutionPausedAtACheckpoint() throws Exception {
        CountDownLatch beforeCheckpoint = new CountDownLatch(1);
        CountDownLatch enterCheckpoint = new CountDownLatch(1);
        Algorithm<TestInput, TestOutput> algorithm = (input, context) -> {
            beforeCheckpoint.countDown();
            enterCheckpoint.await();
            context.checkpoint();
            return new TestOutput(input.value());
        };

        try (LocalAlgorithmExecution execution = execution(Runnable::run)) {
            ExecutionSession session = execution.start(
                    provider("pause", algorithm).invoker(),
                    new TestInput("value"),
                    reducer("", event -> event.payload().getClass().getSimpleName()),
                    state -> {
                    });
            assertTrue(beforeCheckpoint.await(1L, TimeUnit.SECONDS));
            session.pauseExecution();
            enterCheckpoint.countDown();
            Thread.sleep(50L);

            session.cancel();

            ExecutionResult result = session.completion().get(1L, TimeUnit.SECONDS);
            assertEquals(ExecutionStatus.CANCELLED, result.status());
            assertTrue(session.events().stream()
                    .anyMatch(event -> event.payload() instanceof RunCancelledEvent));
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
                    provider("first", echo()).invoker(),
                    new TestInput("first"),
                    reducer("", ExecutionEvent::runId),
                    projectedRunIds::add);
            assertTrue(waitForTask(uiTasks));
            Runnable staleCallback = uiTasks.remove(0);

            ExecutionSession second = execution.start(
                    provider("second", echo()).invoker(),
                    new TestInput("second"),
                    reducer("", ExecutionEvent::runId),
                    projectedRunIds::add);
            assertEquals(ExecutionStatus.COMPLETED, first.completion().get(1L, TimeUnit.SECONDS).status());
            assertTrue(waitForTask(uiTasks));
            drain(uiTasks);
            assertEquals(ExecutionStatus.COMPLETED, second.completion().get(1L, TimeUnit.SECONDS).status());

            staleCallback.run();

            assertFalse(projectedRunIds.contains(first.events().get(0).runId()));
            assertTrue(projectedRunIds.stream()
                    .allMatch(runId -> runId.equals(second.events().get(0).runId())));
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
        Algorithm<TestInput, TestOutput> algorithm = (input, context) -> {
            started.countDown();
            release.await();
            context.checkpoint();
            return new TestOutput(input.value());
        };
        LocalAlgorithmExecution execution = execution(Runnable::run);
        ExecutionSession session = execution.start(
                provider("close", algorithm).invoker(),
                new TestInput("value"),
                reducer("", ExecutionEvent::runId),
                state -> {
                });
        assertTrue(started.await(1L, TimeUnit.SECONDS));

        execution.close();
        release.countDown();

        assertEquals(ExecutionStatus.CANCELLED, session.completion().get(1L, TimeUnit.SECONDS).status());
        assertTrue(session.isClosed());
        assertThrows(IllegalStateException.class, () -> execution.start(
                provider("echo", echo()).invoker(),
                new TestInput("value"),
                reducer("", event -> ""),
                state -> {
                }));
    }

    @Test
    void dispatcherFailureDoesNotLoseAuthoritativeEventsOrFailTheRun() throws Exception {
        LocalAlgorithmExecution execution = execution(runnable -> {
            throw new IllegalStateException("JavaFX is unavailable");
        });
        try (execution) {
            ExecutionSession session = execution.start(
                    provider("echo", echo()).invoker(),
                    new TestInput("value"),
                    reducer("", event -> ""),
                    state -> {
                    });

            ExecutionResult result = session.completion().get(1L, TimeUnit.SECONDS);

            assertEquals(ExecutionStatus.COMPLETED, result.status());
            assertEquals(2, session.events().size());
            assertTrue(session.dispatcherFailure().isPresent());
        }
    }

    @Test
    void eventLimitProducesABoundedAuthoritativeFailureRecord() throws Exception {
        Algorithm<TestInput, TestOutput> noisy = (input, context) -> {
            for (int index = 0; index < 20; index++) {
                context.emit(new TestEvent(index));
            }
            return new TestOutput(input.value());
        };
        try (LocalAlgorithmExecution execution = new LocalAlgorithmExecution(
                new DefaultAlgorithmRunner(), Runnable::run, 8)) {
            ExecutionSession session = execution.start(
                    provider("noisy", noisy).invoker(),
                    new TestInput("value"),
                    reducer("", event -> ""),
                    state -> {
                    });

            ExecutionResult result = session.completion().get(1L, TimeUnit.SECONDS);

            assertEquals(ExecutionStatus.FAILED, result.status());
            assertEquals(8, session.events().size());
            assertEquals("client.execution.event-limit-exceeded", result.failure().orElseThrow().code());
            assertTrue(session.events().getLast().payload() instanceof RunFailedEvent);
        }
    }

    @Test
    void cancellationInterruptsEventDelayAndClosesQueuedObservation() throws Exception {
        List<Runnable> uiTasks = new CopyOnWriteArrayList<>();
        List<Long> projected = new CopyOnWriteArrayList<>();
        CountDownLatch emitted = new CountDownLatch(1);
        Algorithm<TestInput, TestOutput> paced = (input, context) -> {
            emitted.countDown();
            context.emit(new TestEvent(1));
            context.checkpoint();
            return new TestOutput(input.value());
        };
        try (LocalAlgorithmExecution execution = execution(uiTasks::add)) {
            ExecutionSession session = execution.start(
                    provider("paced", paced).invoker(),
                    new TestInput("value"),
                    reducer(-1L, ExecutionEvent::sequence),
                    projected::add,
                    () -> 10_000L);
            assertTrue(emitted.await(1L, TimeUnit.SECONDS));

            long startedAt = System.nanoTime();
            session.cancel();
            ExecutionResult result = session.completion().get(1L, TimeUnit.SECONDS);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            drain(uiTasks);

            assertEquals(ExecutionStatus.CANCELLED, result.status());
            assertTrue(elapsedMillis < 1_000L);
            assertTrue(projected.isEmpty());
            assertTrue(session.events().stream().anyMatch(event -> event.payload() instanceof RunCancelledEvent));
        }
    }

    @Test
    void fatalWorkerErrorStillCompletesTheSessionExceptionally() {
        Algorithm<TestInput, TestOutput> fatal = (input, context) -> {
            throw new AssertionError("fatal");
        };
        try (LocalAlgorithmExecution execution = execution(Runnable::run)) {
            ExecutionSession session = execution.start(
                    provider("fatal", fatal).invoker(),
                    new TestInput("value"),
                    reducer("", event -> ""),
                    state -> {
                    });

            assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> session.completion().get(1L, TimeUnit.SECONDS));
            assertTrue(session.completion().isCompletedExceptionally());
        }
    }

    private LocalAlgorithmExecution execution(java.util.function.Consumer<Runnable> dispatcher) {
        return new LocalAlgorithmExecution(new DefaultAlgorithmRunner(), dispatcher);
    }

    private <S> EventReducer<S> reducer(S initialState, Function<ExecutionEvent, S> mapping) {
        return new EventReducer<>() {
            @Override
            public S initialState() {
                return initialState;
            }

            @Override
            public Reduction<S> reduce(S previousState, ExecutionEvent event) {
                return Reduction.changed(mapping.apply(event), EventImportance.STATE_CHANGE, true);
            }
        };
    }

    private Algorithm<TestInput, TestOutput> echo() {
        return (input, context) -> new TestOutput(input.value());
    }

    private AlgorithmProvider<TestInput, TestOutput> provider(
            String id,
            Algorithm<TestInput, TestOutput> algorithm) {
        return new AlgorithmProvider<>() {
            @Override
            public AlgorithmMetadata metadata() {
                return new AlgorithmMetadata(id, "test", "1");
            }

            @Override
            public Class<TestInput> inputType() {
                return TestInput.class;
            }

            @Override
            public Class<TestOutput> outputType() {
                return TestOutput.class;
            }

            @Override
            public Algorithm<TestInput, TestOutput> createAlgorithm() {
                return algorithm;
            }
        };
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

    private record TestInput(String value) implements AlgorithmInput {
    }

    private record TestOutput(String value) implements AlgorithmOutput {
    }

    private record TestEvent(int value) implements AlgorithmEvent {
    }
}
