package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmInvocationException;
import com.majortom.algorithms.core.api.AlgorithmInvoker;
import com.majortom.algorithms.core.api.AlgorithmOutput;
import com.majortom.algorithms.core.api.PreparedAlgorithmInvocation;
import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.domain.execution.RunStartedEvent;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Common synchronous execution orchestration shared by desktop, server, and tests. */
public final class DefaultAlgorithmRunner {

    private final Clock clock;
    private final Supplier<String> runIdSupplier;

    public DefaultAlgorithmRunner() {
        this(Clock.systemUTC(), () -> UUID.randomUUID().toString());
    }

    public DefaultAlgorithmRunner(Clock clock, Supplier<String> runIdSupplier) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.runIdSupplier = Objects.requireNonNull(runIdSupplier, "runIdSupplier");
    }

    public ExecutionResult run(AlgorithmInvoker invoker, AlgorithmInput input, EventSink eventSink) {
        return run(invoker, input, eventSink, RunControl.unrestricted());
    }

    public ExecutionResult run(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventSink eventSink,
            RunControl runControl) {
        Objects.requireNonNull(invoker, "invoker");
        Objects.requireNonNull(eventSink, "eventSink");
        Objects.requireNonNull(runControl, "runControl");
        DefaultAlgorithmContext context;
        try {
            String runId = Objects.requireNonNull(runIdSupplier.get(), "runIdSupplier result");
            String algorithmId = Objects.requireNonNull(invoker.metadata(), "invoker metadata").id();
            context = new DefaultAlgorithmContext(
                    runId,
                    algorithmId,
                    eventSink,
                    runControl,
                    runControl,
                    clock);
        } catch (RuntimeException exception) {
            return initializationFailed(exception);
        }

        PreparedAlgorithmInvocation invocation;
        try {
            invocation = invoker.prepare(input);
        } catch (AlgorithmInvocationException exception) {
            return failed(context, exception.code(), exception);
        } catch (RuntimeException exception) {
            return failed(context, "algorithm.input.invalid", exception);
        }

        try {
            context.emitLifecycle(new RunStartedEvent());
            context.checkpoint();
            AlgorithmOutput output = invocation.invoke(context);
            context.checkpoint();
            context.emitLifecycle(new RunCompletedEvent());
            return ExecutionResult.completed(output);
        } catch (AlgorithmCancellationException exception) {
            return cancelled(context, exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return cancelled(context, "Algorithm execution interrupted");
        } catch (EventDeliveryException exception) {
            return eventDeliveryFailed(exception);
        } catch (AlgorithmInvocationException exception) {
            return failed(context, exception.code(), exception);
        } catch (RuntimeException exception) {
            return failed(context, "algorithm.execution.failed", exception);
        }
    }

    private ExecutionResult failed(DefaultAlgorithmContext context, String code, RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        try {
            context.emitLifecycle(new RunFailedEvent(code, message));
        } catch (EventDeliveryException deliveryException) {
            return eventDeliveryFailed(deliveryException);
        }
        ExecutionFailure failure = new ExecutionFailure(code, message, exception.getClass().getName());
        return ExecutionResult.failed(failure);
    }

    private ExecutionResult eventDeliveryFailed(EventDeliveryException exception) {
        Throwable cause = exception.getCause();
        String exceptionType = exception.getClass().getName();
        if (cause != null) {
            exceptionType = cause.getClass().getName();
        }
        ExecutionFailure failure = new ExecutionFailure(
                "execution.event-delivery.failed",
                exception.getMessage(),
                exceptionType);
        return ExecutionResult.failed(failure);
    }

    private ExecutionResult initializationFailed(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        ExecutionFailure failure = new ExecutionFailure(
                "execution.initialization.failed",
                message,
                exception.getClass().getName());
        return ExecutionResult.failed(failure);
    }

    private ExecutionResult cancelled(DefaultAlgorithmContext context, String reason) {
        try {
            context.emitLifecycle(new RunCancelledEvent(reason));
        } catch (EventDeliveryException exception) {
            return eventDeliveryFailed(exception);
        }
        return ExecutionResult.cancelled();
    }
}
