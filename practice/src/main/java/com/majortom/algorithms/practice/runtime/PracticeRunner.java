package com.majortom.algorithms.practice.runtime;

import com.majortom.algorithms.core.memory.JdkMemoryProfiler;
import com.majortom.algorithms.core.memory.MemoryDomain;
import com.majortom.algorithms.core.memory.MemoryProfile;
import com.majortom.algorithms.core.memory.MemoryProfileSession;
import com.majortom.algorithms.core.memory.MemoryProfileStore;
import com.majortom.algorithms.core.memory.MemoryProfiler;
import com.majortom.algorithms.practice.runtime.model.ProblemDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;

public final class PracticeRunner {
  private final MemoryProfiler memoryProfiler;
  private final MemoryProfileStore memoryProfiles = new MemoryProfileStore();
  private volatile MemoryProfile lastMemoryProfile;

  public PracticeRunner() {
    this(JdkMemoryProfiler.shared());
  }

  PracticeRunner(MemoryProfiler memoryProfiler) {
    this.memoryProfiler = Objects.requireNonNull(memoryProfiler, "memoryProfiler");
  }

  public Object run(ProblemDescriptor descriptor, Object... arguments) {
    Objects.requireNonNull(descriptor, "descriptor");
    Method entry = descriptor.entryPoint();
    Object receiver = null;
    try {
      if (!Modifier.isStatic(entry.getModifiers())) {
        receiver = descriptor.implementation().getDeclaredConstructor().newInstance();
      }
      MemoryProfileSession memory =
          memoryProfiler.begin(MemoryDomain.PRACTICE, descriptor.stableId());
      try {
        return entry.invoke(receiver, arguments);
      } finally {
        memory.close();
        lastMemoryProfile = memory.snapshot();
        memoryProfiles.record(lastMemoryProfile);
      }
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtime)
        throw runtime;
      if (cause instanceof Error error)
        throw error;
      throw new PracticeExecutionException("Problem entry failed: " + descriptor.stableId(), cause);
    } catch (ReflectiveOperationException exception) {
      throw new PracticeExecutionException(
          "Unable to invoke problem entry: " + descriptor.stableId(), exception);
    }
  }

  public Optional<MemoryProfile> lastMemoryProfile() {
    return Optional.ofNullable(lastMemoryProfile);
  }

  public MemoryProfileStore memoryProfiles() {
    return memoryProfiles;
  }

  public static final class PracticeExecutionException extends RuntimeException {
    public PracticeExecutionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
