package com.majortom.algorithms.practice.runtime.worker;

import com.majortom.algorithms.core.memory.MemoryProfile;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PracticeRecording(List<PracticeFrame> frames, List<PracticeExceptionFact> exceptions,
    Object result, int exitCode, boolean timedOut, String stderr,
    Optional<MemoryProfile> memoryProfile) {
  public PracticeRecording {
    frames = List.copyOf(frames);
    exceptions = List.copyOf(exceptions);
    stderr = stderr == null ? "" : stderr;
    memoryProfile = Objects.requireNonNull(memoryProfile, "memoryProfile");
  }
}
