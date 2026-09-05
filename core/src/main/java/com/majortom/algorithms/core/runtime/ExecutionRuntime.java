package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.domain.execution.RunStartedEvent;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Small runtime lifecycle owner; domain behavior remains in domain methods. */
public final class ExecutionRuntime {
    private final Clock clock;
    private final Supplier<String> runIdSupplier;

    public ExecutionRuntime() {
        this(Clock.systemUTC(), () -> UUID.randomUUID().toString());
    }

    public ExecutionRuntime(Clock clock, Supplier<String> runIdSupplier) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.runIdSupplier = Objects.requireNonNull(runIdSupplier, "runIdSupplier");
    }

    public ExecutionResult execute(
            String operationId,
            EventSink sink,
            ExecutionOperation<?> operation) {
        return execute(operationId, operationId, sink, RunControl.unrestricted(), operation);
    }

    public ExecutionResult execute(
            String operationId,
            EventSink sink,
            RunControl control,
            ExecutionOperation<?> operation) {
        return execute(operationId, operationId, sink, control, operation);
    }

    public ExecutionResult execute(
            String operationId,
            String source,
            EventSink sink,
            RunControl control,
            ExecutionOperation<?> operation) {
        Objects.requireNonNull(operation, "operation");
        RuntimeEventContext context = new RuntimeEventContext(
                runIdSupplier.get(), operationId, source, sink, control, clock);
        DefaultExecutionControl defaultControl = control instanceof DefaultExecutionControl value ? value : null;
        if (defaultControl != null) {
            defaultControl.bindLifecycle(context::emitLifecycle);
        }

        try {
            context.emitLifecycle(new RunStartedEvent());
            context.startCheckpoint();
            Object output;
            try (ExecutionEvents.Binding ignored = ExecutionEvents.bind(context)) {
                output = operation.execute();
            }
            context.completionCheckpoint();
            context.emitLifecycle(new RunCompletedEvent());
            return ExecutionResult.completed(output);
        } catch (ExecutionCancellationException exception) {
            return cancelled(context, exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return cancelled(context, "Execution interrupted");
        } catch (EventDeliveryException exception) {
            return eventDeliveryFailure(exception);
        } catch (RuntimeException exception) {
            try {
                context.emitLifecycle(new RunFailedEvent("execution.operation.failed", message(exception)));
            } catch (EventDeliveryException deliveryFailure) {
                return eventDeliveryFailure(deliveryFailure);
            }
            return ExecutionResult.failed(new ExecutionFailure(
                    "execution.operation.failed", message(exception), exception.getClass().getName()));
        } finally {
            if (defaultControl != null) {
                defaultControl.unbindLifecycle();
            }
        }
    }

    private ExecutionResult cancelled(RuntimeEventContext context, String reason) {
        try {
            context.emitLifecycle(new RunCancelledEvent(
                    reason == null ? "Execution cancelled" : reason));
        } catch (EventDeliveryException deliveryFailure) {
            return eventDeliveryFailure(deliveryFailure);
        }
        return ExecutionResult.cancelled();
    }

    private static ExecutionResult eventDeliveryFailure(EventDeliveryException exception) {
        return ExecutionResult.failed(new ExecutionFailure(
                "execution.event.delivery.failed", message(exception), exception.getClass().getName()));
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
