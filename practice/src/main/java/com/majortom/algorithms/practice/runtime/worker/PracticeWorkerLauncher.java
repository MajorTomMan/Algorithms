package com.majortom.algorithms.practice.runtime.worker;

import com.majortom.algorithms.telemetry.api.TelemetryProfile;
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
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Executes one already-discovered Practice entry in an isolated child JVM and records factual JDI
 * trace frames.
 */
public final class PracticeWorkerLauncher {
  private static final int TIMEOUT_EXIT_CODE = 124;
  private static final int TRACE_LIMIT_EXIT_CODE = 125;
  private static final int MAX_FRAMES = 50_000;
  private static final int MAX_EXCEPTIONS = 1_000;
  private static final int MAX_OUTPUT_BYTES = 4_194_304;

  public PracticeRecording run(
      ProblemDescriptor descriptor, Duration timeout, Object... arguments) {
    Objects.requireNonNull(descriptor, "descriptor");
    Objects.requireNonNull(timeout, "timeout");
    if (timeout.isZero() || timeout.isNegative())
      throw new IllegalArgumentException("timeout must be positive");
    Method entry = descriptor.entryPoint();
    String[] parameterNames = java.util.Arrays.stream(entry.getParameterTypes())
                                  .map(Class::getName)
                                  .toArray(String[] ::new);
    WorkerInvocation invocation = new WorkerInvocation(
        descriptor.stableId(), descriptor.implementation().getName(), entry.getName(), parameterNames, arguments);
    return launch(invocation, timeout);
  }

  private PracticeRecording launch(WorkerInvocation invocation, Duration timeout) {
    VirtualMachine vm = null;
    Process worker = null;
    ExecutorService outputReaders = null;
    Future<OutputCapture> stdoutReader = null;
    Future<OutputCapture> stderrReader = null;
    List<PracticeFrame> frames = new ArrayList<>();
    List<PracticeExceptionFact> exceptions = new ArrayList<>();
    boolean timedOut = false;
    boolean traceLimitExceeded = false;
    int exitCode = -1;
    try {
      LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
      Map<String, Connector.Argument> connectorArgs = connector.defaultArguments();
      String encodedInvocation = WorkerCodec.encode(invocation);
      connectorArgs.get("main").setValue(
          PracticeWorkerMain.class.getName() + " " + encodedInvocation);
      connectorArgs.get("options").setValue(
          "-Dfile.encoding=UTF-8 -cp \"" + childClasspath() + "\"");
      connectorArgs.get("suspend").setValue("true");
      vm = connector.launch(connectorArgs);
      worker = vm.process();
      outputReaders = Executors.newFixedThreadPool(2, task -> {
        Thread reader = new Thread(task, "practice-worker-output");
        reader.setDaemon(true);
        return reader;
      });
      Process child = worker;
      stdoutReader = outputReaders.submit(() -> readLimited(child.getInputStream()));
      stderrReader = outputReaders.submit(() -> readLimited(child.getErrorStream()));

      ClassPrepareRequest prepare = vm.eventRequestManager().createClassPrepareRequest();
      prepare.addClassFilter(invocation.className());
      prepare.setSuspendPolicy(EventRequest.SUSPEND_ALL);
      prepare.enable();

      ExceptionRequest exceptionRequest =
          vm.eventRequestManager().createExceptionRequest(null, true, true);
      exceptionRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
      exceptionRequest.enable();

      long deadline = System.nanoTime() + timeout.toNanos();
      boolean done = false;
      vm.resume();
      while (!done && System.nanoTime() < deadline) {
        long remainingMillis = Math.max(
            1L, Math.min(100L, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())));
        EventSet set;
        try {
          set = vm.eventQueue().remove(remainingMillis);
        } catch (VMDisconnectedException disconnected) {
          break;
        }
        if (set == null)
          continue;
        try {
          for (Event event : set) {
            if (event instanceof ClassPrepareEvent prepared) {
              installStepRequest(vm, prepared.thread(), invocation.className());
            } else if (event instanceof StepEvent step) {
              if (step.location().declaringType().name().equals(invocation.className())) {
                if (frames.size() >= MAX_FRAMES) {
                  traceLimitExceeded = true;
                  done = true;
                  break;
                }
                frames.add(frame(step));
              }
            } else if (event instanceof ExceptionEvent exception) {
              if (exception.location().declaringType().name().equals(invocation.className())) {
                if (exceptions.size() >= MAX_EXCEPTIONS) {
                  traceLimitExceeded = true;
                  done = true;
                  break;
                }
                exceptions.add(
                    new PracticeExceptionFact(exception.exception().referenceType().name(),
                        exceptionMessage(exception.exception()), exception.location().lineNumber(),
                        exception.catchLocation() != null));
              }
            } else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
              done = true;
            }
          }
        } finally {
          try {
            set.resume();
          } catch (VMDisconnectedException ignored) {
            done = true;
          }
        }
      }

      if (traceLimitExceeded && worker.isAlive()) {
        terminate(vm, worker, TRACE_LIMIT_EXIT_CODE);
        exceptions.add(new PracticeExceptionFact("PracticeTraceLimit",
            "Practice trace exceeded the configured frame/exception limit", -1, false));
      }
      if (!traceLimitExceeded && worker.isAlive() && System.nanoTime() >= deadline) {
        timedOut = true;
        terminate(vm, worker, TIMEOUT_EXIT_CODE);
      }
      if (worker.isAlive() && !worker.waitFor(3, TimeUnit.SECONDS)) {
        worker.destroyForcibly();
        worker.waitFor(3, TimeUnit.SECONDS);
      }
      if (!worker.isAlive()) {
        exitCode = worker.exitValue();
      }
      OutputCapture out = stdoutReader.get(3, TimeUnit.SECONDS);
      OutputCapture err = stderrReader.get(3, TimeUnit.SECONDS);
      if (out.truncated() || err.truncated()) {
        exceptions.add(new PracticeExceptionFact("PracticeOutputLimit",
            "Worker output exceeded the capture limit; excess output was discarded", -1, false));
      }
      Object result = timedOut || traceLimitExceeded || out.truncated()
          ? null : parseResult(out.text());
      TelemetryProfile memoryProfile = timedOut || traceLimitExceeded || out.truncated()
          ? null : parseMemoryProfile(out.text());
      if (memoryProfile != null) {
        memoryProfile = memoryProfile.withoutRepresentativeTiming();
      }
      return new PracticeRecording(frames, exceptions, result,
          traceLimitExceeded ? TRACE_LIMIT_EXIT_CODE : timedOut ? TIMEOUT_EXIT_CODE : exitCode,
          timedOut, err.text(), java.util.Optional.ofNullable(memoryProfile));
    } catch (Exception exception) {
      if (worker != null && worker.isAlive()) {
        worker.destroyForcibly();
      }
      if (exception instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new IllegalStateException(
          "Practice worker execution failed for " + invocation, exception);
    } finally {
      if (outputReaders != null) {
        outputReaders.shutdownNow();
      }
      if (vm != null) {
        try {
          vm.dispose();
        } catch (VMDisconnectedException ignored) {
          // Normal after the worker exits.
        }
      }
    }
  }

  private static void installStepRequest(
      VirtualMachine vm, ThreadReference thread, String targetClass) {
    StepRequest step = vm.eventRequestManager().createStepRequest(
        thread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
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

  private static String value(
      Value value, int depth, IdentityHashMap<ObjectReference, Boolean> seen) {
    if (value == null)
      return "null";
    if (value instanceof PrimitiveValue primitive)
      return primitive.toString();
    if (value instanceof StringReference string)
      return '"' + string.value() + '"';
    if (value instanceof ArrayReference array) {
      int limit = Math.min(array.length(), 16);
      List<String> values = new ArrayList<>(limit);
      for (int i = 0; i < limit; i++) values.add(value(array.getValue(i), depth + 1, seen));
      if (array.length() > limit)
        values.add("…");
      return "[" + String.join(",", values) + "]";
    }
    if (value instanceof ObjectReference object) {
      ReferenceType type = object.referenceType();
      String identity = type.name() + "#" + object.uniqueID();
      if (depth >= 1 || seen.put(object, Boolean.TRUE) != null)
        return identity;
      List<Field> fields =
          type.allFields().stream().filter(field -> !field.isStatic()).limit(8).toList();
      if (fields.isEmpty())
        return identity;
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
    if (surefire != null && !surefire.isBlank())
      return surefire;
    return System.getProperty("java.class.path");
  }

  private static void terminate(VirtualMachine vm, Process worker, int exitCode) {
    try {
      vm.exit(exitCode);
    } catch (Exception ignored) {
      worker.destroyForcibly();
    }
    try {
      if (!worker.waitFor(3, TimeUnit.SECONDS))
        worker.destroyForcibly();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      worker.destroyForcibly();
    }
  }

  private static OutputCapture readLimited(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    boolean truncated = false;
    int count;
    while ((count = input.read(chunk)) != -1) {
      int remaining = MAX_OUTPUT_BYTES - output.size();
      if (remaining > 0) {
        output.write(chunk, 0, Math.min(count, remaining));
      }
      if (count > remaining) {
        truncated = true;
      }
      // Continue draining even after reaching the capture limit, so the child
      // cannot block on a full stdout/stderr pipe.
    }
    return new OutputCapture(output.toString(StandardCharsets.UTF_8), truncated);
  }

  private record OutputCapture(String text, boolean truncated) {}

  private static Object parseResult(String stdout) {
    for (String line : stdout.lines().toList()) {
      if (line.startsWith(PracticeWorkerMain.RESULT_PREFIX)) {
        return WorkerCodec.decodeResult(line.substring(PracticeWorkerMain.RESULT_PREFIX.length()));
      }
    }
    return null;
  }

  private static TelemetryProfile parseMemoryProfile(String stdout) {
    for (String line : stdout.lines().toList()) {
      if (line.startsWith(PracticeWorkerMain.MEMORY_PREFIX)) {
        return WorkerCodec.decodeProfile(line.substring(PracticeWorkerMain.MEMORY_PREFIX.length()));
      }
    }
    return null;
  }
}
