package com.majortom.algorithms.practice.runtime.worker;

import com.majortom.algorithms.practice.runtime.model.ProblemDescriptor;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.StepRequest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Executes one already-discovered Practice entry in an isolated child JVM and records factual JDI trace frames. */
public final class PracticeWorkerLauncher {
    private static final int TIMEOUT_EXIT_CODE = 124;

    public PracticeRecording run(ProblemDescriptor descriptor, Duration timeout, Object... arguments) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        Method entry = descriptor.entryPoint();
        String[] parameterNames = java.util.Arrays.stream(entry.getParameterTypes()).map(Class::getName).toArray(String[]::new);
        WorkerInvocation invocation = new WorkerInvocation(
                descriptor.implementation().getName(), entry.getName(), parameterNames, arguments);
        return launch(invocation, timeout);
    }

    private PracticeRecording launch(WorkerInvocation invocation, Duration timeout) {
        VirtualMachine vm = null;
        Process worker = null;
        List<PracticeFrame> frames = new ArrayList<>();
        List<PracticeExceptionFact> exceptions = new ArrayList<>();
        boolean timedOut = false;
        int exitCode = -1;
        try {
            LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
            Map<String, Connector.Argument> connectorArgs = connector.defaultArguments();
            String encodedInvocation = WorkerCodec.encode(invocation);
            connectorArgs.get("main").setValue(PracticeWorkerMain.class.getName() + " " + encodedInvocation);
            connectorArgs.get("options").setValue("-Dfile.encoding=UTF-8 -cp \"" + childClasspath() + "\"");
            connectorArgs.get("suspend").setValue("true");
            vm = connector.launch(connectorArgs);
            worker = vm.process();

            ClassPrepareRequest prepare = vm.eventRequestManager().createClassPrepareRequest();
            prepare.addClassFilter(invocation.className());
            prepare.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            prepare.enable();

            ExceptionRequest exceptionRequest = vm.eventRequestManager().createExceptionRequest(null, true, true);
            exceptionRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            exceptionRequest.enable();

            long deadline = System.nanoTime() + timeout.toNanos();
            boolean done = false;
            vm.resume();
            while (!done && System.nanoTime() < deadline) {
                long remainingMillis = Math.max(1L,
                        Math.min(100L, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())));
                EventSet set;
                try {
                    set = vm.eventQueue().remove(remainingMillis);
                } catch (VMDisconnectedException disconnected) {
                    break;
                }
                if (set == null) continue;
                try {
                    for (Event event : set) {
                        if (event instanceof ClassPrepareEvent prepared) {
                            installStepRequest(vm, prepared.thread(), invocation.className());
                        } else if (event instanceof StepEvent step) {
                            if (step.location().declaringType().name().equals(invocation.className())) {
                                frames.add(frame(step));
                            }
                        } else if (event instanceof ExceptionEvent exception) {
                            if (exception.location().declaringType().name().equals(invocation.className())) {
                                exceptions.add(new PracticeExceptionFact(
                                        exception.exception().referenceType().name(),
                                        exceptionMessage(exception.exception()),
                                        exception.location().lineNumber(),
                                        exception.catchLocation() != null));
                            }
                        } else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                            done = true;
                        }
                    }
                } finally {
                    try { set.resume(); } catch (VMDisconnectedException ignored) { done = true; }
                }
            }

            if (worker.isAlive() && System.nanoTime() >= deadline) {
                timedOut = true;
                terminate(vm, worker);
            }
            if (worker.isAlive()) worker.waitFor(3, TimeUnit.SECONDS);
            if (worker.isAlive()) worker.destroyForcibly();
            if (!worker.isAlive()) exitCode = worker.exitValue();

            String stdout = read(worker.getInputStream());
            String stderr = read(worker.getErrorStream());
            Object result = timedOut ? null : parseResult(stdout);
            return new PracticeRecording(frames, exceptions, result,
                    timedOut ? TIMEOUT_EXIT_CODE : exitCode, timedOut, stderr);
        } catch (Exception exception) {
            if (worker != null && worker.isAlive()) worker.destroyForcibly();
            throw new IllegalStateException("Practice worker execution failed for " + invocation, exception);
        }
    }

    private static void installStepRequest(VirtualMachine vm, ThreadReference thread, String targetClass) {
        StepRequest step = vm.eventRequestManager().createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
        step.addClassFilter(targetClass);
        step.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        step.enable();
    }

    private static PracticeFrame frame(StepEvent step) {
        StackFrame frame;
        try {
            frame = step.thread().frame(0);
        } catch (Exception unavailable) {
            return new PracticeFrame(sourceName(step), step.location().declaringType().name(),
                    step.location().method().name(), step.location().lineNumber(), Map.of());
        }
        LinkedHashMap<String, String> locals = new LinkedHashMap<>();
        try {
            List<LocalVariable> variables = new ArrayList<>(frame.visibleVariables());
            variables.sort(Comparator.comparing(LocalVariable::name));
            for (LocalVariable variable : variables) {
                locals.put(variable.name(), value(frame.getValue(variable), 0, new IdentityHashMap<>()));
            }
        } catch (AbsentInformationException ignored) {
            // Debug information is optional; source line recording remains valid without locals.
        }
        return new PracticeFrame(sourceName(step), step.location().declaringType().name(),
                step.location().method().name(), step.location().lineNumber(), locals);
    }

    private static String sourceName(StepEvent step) {
        try {
            return step.location().sourceName();
        } catch (AbsentInformationException unavailable) {
            return "<unknown>";
        }
    }

    private static String value(Value value, int depth, IdentityHashMap<ObjectReference, Boolean> seen) {
        if (value == null) return "null";
        if (value instanceof PrimitiveValue primitive) return primitive.toString();
        if (value instanceof StringReference string) return '"' + string.value() + '"';
        if (value instanceof ArrayReference array) {
            int limit = Math.min(array.length(), 16);
            List<String> values = new ArrayList<>(limit);
            for (int i = 0; i < limit; i++) values.add(value(array.getValue(i), depth + 1, seen));
            if (array.length() > limit) values.add("…");
            return "[" + String.join(",", values) + "]";
        }
        if (value instanceof ObjectReference object) {
            ReferenceType type = object.referenceType();
            String identity = type.name() + "#" + object.uniqueID();
            if (depth >= 1 || seen.put(object, Boolean.TRUE) != null) return identity;
            List<Field> fields = type.allFields().stream().filter(field -> !field.isStatic()).limit(8).toList();
            if (fields.isEmpty()) return identity;
            List<String> facts = new ArrayList<>(fields.size());
            for (Field field : fields) {
                Value fieldValue = object.getValue(field);
                if (fieldValue instanceof ObjectReference ref && ref.uniqueID() == object.uniqueID()) {
                    facts.add(field.name() + "=self");
                } else {
                    facts.add(field.name() + "=" + value(fieldValue, depth + 1, seen));
                }
            }
            return identity + "(" + String.join(",", facts) + ")";
        }
        return String.valueOf(value);
    }

    private static String exceptionMessage(ObjectReference exception) {
        try {
            Field message = exception.referenceType().fieldByName("detailMessage");
            Value value = message == null ? null : exception.getValue(message);
            return value instanceof StringReference text ? text.value() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String childClasspath() {
        String surefire = System.getProperty("surefire.test.class.path");
        if (surefire != null && !surefire.isBlank()) return surefire;
        return System.getProperty("java.class.path");
    }

    private static void terminate(VirtualMachine vm, Process worker) {
        try {
            vm.exit(TIMEOUT_EXIT_CODE);
        } catch (Exception ignored) {
            worker.destroyForcibly();
        }
        try {
            if (!worker.waitFor(3, TimeUnit.SECONDS)) worker.destroyForcibly();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            worker.destroyForcibly();
        }
    }

    private static String read(java.io.InputStream input) throws IOException {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static Object parseResult(String stdout) {
        for (String line : stdout.lines().toList()) {
            if (line.startsWith(PracticeWorkerMain.RESULT_PREFIX)) {
                return WorkerCodec.decode(line.substring(PracticeWorkerMain.RESULT_PREFIX.length()));
            }
        }
        return null;
    }
}
