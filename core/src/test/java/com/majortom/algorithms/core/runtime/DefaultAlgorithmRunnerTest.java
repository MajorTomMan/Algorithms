package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.api.Algorithm;
import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmMetadata;
import com.majortom.algorithms.core.api.AlgorithmOutput;
import com.majortom.algorithms.core.api.AlgorithmProvider;
import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunStartedEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAlgorithmRunnerTest {

    @Test
    void wrapsLifecycleWithMonotonicSequenceAndStableMetadata() {
        InMemoryEventSink sink = new InMemoryEventSink();
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        DefaultAlgorithmRunner runner = new DefaultAlgorithmRunner(clock, () -> "run-1");

        ExecutionResult result = runner.run(new EchoProvider().invoker(), new TestInput("hello"), sink);

        assertEquals(ExecutionStatus.COMPLETED, result.status());
        assertEquals(new TestOutput("hello"), result.output().orElseThrow());
        List<ExecutionEvent> events = sink.events();
        assertEquals(2, events.size());
        assertInstanceOf(RunStartedEvent.class, events.get(0).payload());
        assertInstanceOf(RunCompletedEvent.class, events.get(1).payload());
        assertEquals(List.of(0L, 1L), events.stream().map(ExecutionEvent::sequence).toList());
        assertTrue(events.stream().allMatch(event -> event.runId().equals("run-1")));
        assertTrue(events.stream().allMatch(event -> event.algorithmId().equals("echo")));
    }

    @Test
    void cancellationWakesAPausedExecutionWithoutPolling() throws Exception {
        DefaultExecutionControl control = new DefaultExecutionControl();
        control.pause();
        InMemoryEventSink memory = new InMemoryEventSink();
        CountDownLatch started = new CountDownLatch(1);
        EventSink sink = event -> {
            memory.accept(event);
            if (event.payload() instanceof RunStartedEvent) {
                started.countDown();
            }
        };
        AtomicReference<ExecutionResult> result = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            DefaultAlgorithmRunner runner = new DefaultAlgorithmRunner();
            result.set(runner.run(new EchoProvider().invoker(), new TestInput("hello"), sink, control));
        });

        worker.start();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        control.cancel();
        worker.join(2_000L);

        assertTrue(!worker.isAlive());
        assertEquals(ExecutionStatus.CANCELLED, result.get().status());
        assertInstanceOf(RunCancelledEvent.class, memory.events().get(memory.events().size() - 1).payload());
    }

    @Test
    void interruptionBecomesCancellationAndRestoresTheInterruptFlag() throws Exception {
        CountDownLatch waitingAtGate = new CountDownLatch(1);
        CountDownLatch blocker = new CountDownLatch(1);
        RunControl control = new RunControl() {
            @Override
            public boolean isCancellationRequested() {
                return false;
            }

            @Override
            public void awaitPermission(CancellationToken cancellationToken) throws InterruptedException {
                waitingAtGate.countDown();
                blocker.await();
            }
        };
        AtomicReference<ExecutionResult> result = new AtomicReference<>();
        AtomicReference<Boolean> interruptRestored = new AtomicReference<>(false);
        Thread worker = new Thread(() -> {
            DefaultAlgorithmRunner runner = new DefaultAlgorithmRunner();
            result.set(runner.run(
                    new EchoProvider().invoker(),
                    new TestInput("hello"),
                    EventSink.noop(),
                    control));
            interruptRestored.set(Thread.currentThread().isInterrupted());
        });

        worker.start();
        assertTrue(waitingAtGate.await(2, TimeUnit.SECONDS));
        worker.interrupt();
        worker.join(2_000L);

        assertTrue(!worker.isAlive());
        assertEquals(ExecutionStatus.CANCELLED, result.get().status());
        assertTrue(interruptRestored.get());
    }

    @Test
    void rejectsLifecycleEventsForgedByAnAlgorithm() {
        Algorithm<TestInput, TestOutput> malicious = (input, context) -> {
            context.emit(new RunCompletedEvent());
            return new TestOutput(input.value());
        };
        AlgorithmProvider<TestInput, TestOutput> provider = providerFor(malicious);

        ExecutionResult result = new DefaultAlgorithmRunner().run(
                provider.invoker(),
                new TestInput("hello"),
                EventSink.noop());

        assertEquals(ExecutionStatus.FAILED, result.status());
        assertEquals("algorithm.execution.failed", result.failure().orElseThrow().code());
    }

    @Test
    void reportsEventDeliveryFailureWithoutRetryingTheBrokenSink() {
        EventSink brokenSink = event -> {
            throw new IllegalStateException("sink unavailable");
        };

        ExecutionResult result = new DefaultAlgorithmRunner().run(
                new EchoProvider().invoker(),
                new TestInput("hello"),
                brokenSink);

        assertEquals(ExecutionStatus.FAILED, result.status());
        assertEquals("execution.event-delivery.failed", result.failure().orElseThrow().code());
    }

    @Test
    void reportsDeliveryFailureWhenCancellationEventCannotBePublished() {
        DefaultExecutionControl control = new DefaultExecutionControl();
        control.cancel();
        EventSink sink = event -> {
            if (event.payload() instanceof RunCancelledEvent) {
                throw new IllegalStateException("terminal sink failure");
            }
        };

        ExecutionResult result = new DefaultAlgorithmRunner().run(
                new EchoProvider().invoker(),
                new TestInput("hello"),
                sink,
                control);

        assertEquals(ExecutionStatus.FAILED, result.status());
        assertEquals("execution.event-delivery.failed", result.failure().orElseThrow().code());
    }

    @Test
    void initializationFailuresUseTheResultChannel() {
        DefaultAlgorithmRunner runner = new DefaultAlgorithmRunner(
                Clock.systemUTC(),
                () -> {
                    throw new IllegalStateException("run ID unavailable");
                });

        ExecutionResult result = runner.run(
                new EchoProvider().invoker(),
                new TestInput("hello"),
                EventSink.noop());

        assertEquals(ExecutionStatus.FAILED, result.status());
        assertEquals("execution.initialization.failed", result.failure().orElseThrow().code());
    }

    private AlgorithmProvider<TestInput, TestOutput> providerFor(Algorithm<TestInput, TestOutput> algorithm) {
        return new AlgorithmProvider<>() {
            @Override
            public AlgorithmMetadata metadata() {
                return new AlgorithmMetadata("malicious", "test", "1");
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

    private record TestInput(String value) implements AlgorithmInput {
    }

    private record TestOutput(String value) implements AlgorithmOutput {
    }

    private static final class EchoProvider implements AlgorithmProvider<TestInput, TestOutput> {

        @Override
        public AlgorithmMetadata metadata() {
            return new AlgorithmMetadata("echo", "test", "1");
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
            return (input, context) -> new TestOutput(input.value());
        }
    }
}
