package com.majortom.algorithms.practice.runtime;

import com.majortom.algorithms.practice.runtime.model.ProblemDescriptor;
import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import com.majortom.algorithms.telemetry.memory.runtime.MemoryTelemetryRun;
import com.majortom.algorithms.telemetry.memory.runtime.MemoryTelemetryService;
import com.majortom.algorithms.telemetry.runtime.TelemetryStore;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;

public final class PracticeRunner {
  private final MemoryTelemetryService memoryTelemetry;
  private volatile TelemetryProfile lastMemoryProfile;

  public PracticeRunner() {
    this(MemoryTelemetryService.shared());
  }

  PracticeRunner(MemoryTelemetryService memoryTelemetry) {
    this.memoryTelemetry = Objects.requireNonNull(memoryTelemetry, "memoryTelemetry");
  }

  public Object run(ProblemDescriptor descriptor, Object... arguments) {
    Objects.requireNonNull(descriptor, "descriptor");
    Method entry = descriptor.entryPoint();
    Object receiver = null;
    try {
      if (!Modifier.isStatic(entry.getModifiers())) {
        receiver = descriptor.implementation().getDeclaredConstructor().newInstance();
      }
      MemoryTelemetryRun memory =
          memoryTelemetry.begin(TelemetryScopeId.practice(descriptor.stableId()), false);
      try {
        Object result = entry.invoke(receiver, arguments);
        memory.complete();
        return result;
      } catch (InvocationTargetException exception) {
        memory.fail();
        throw exception;
      } catch (RuntimeException | Error exception) {
        memory.fail();
        throw exception;
      } finally {
        if (!memory.finished()) memory.cancel();
        lastMemoryProfile = memory.snapshot();
      }
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtime) throw runtime;
      if (cause instanceof Error error) throw error;
      throw new PracticeExecutionException("Problem entry failed: " + descriptor.stableId(), cause);
    } catch (ReflectiveOperationException exception) {
      throw new PracticeExecutionException(
          "Unable to invoke problem entry: " + descriptor.stableId(), exception);
    }
  }

  public Optional<TelemetryProfile> lastMemoryProfile() {
    return Optional.ofNullable(lastMemoryProfile);
  }

  public TelemetryStore memoryProfiles() {
    return memoryTelemetry.store();
  }

  public static final class PracticeExecutionException extends RuntimeException {
    public PracticeExecutionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
