package com.majortom.algorithms.practice.runtime.worker;

import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PracticeRecording(List<PracticeFrame> frames, List<PracticeExceptionFact> exceptions,
    Object result, int exitCode, boolean timedOut, String stderr,
    Optional<TelemetryProfile> memoryProfile) {
  public PracticeRecording {
    frames = List.copyOf(frames);
    exceptions = List.copyOf(exceptions);
    stderr = stderr == null ? "" : stderr;
    memoryProfile = Objects.requireNonNull(memoryProfile, "memoryProfile");
  }
}
