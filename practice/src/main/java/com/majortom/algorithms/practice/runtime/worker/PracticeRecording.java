package com.majortom.algorithms.practice.runtime.worker;

import java.util.List;

public record PracticeRecording(List<PracticeFrame> frames, List<PracticeExceptionFact> exceptions,
                                Object result, int exitCode, boolean timedOut, String stderr) {
    public PracticeRecording {
        frames = List.copyOf(frames);
        exceptions = List.copyOf(exceptions);
        stderr = stderr == null ? "" : stderr;
    }
}
