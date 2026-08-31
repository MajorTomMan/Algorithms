package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.core.runtime.ExecutionResult;

import java.util.Objects;

/** Client history entry that decorates the shared recording with presentation identity and frame statistics. */
public record ClientExecutionRecord(
        String moduleId,
        String algorithmId,
        String inputFingerprint,
        ExecutionResult result,
        ExecutionRecording recording,
        long visualFrameCount) {

    public ClientExecutionRecord {
        moduleId = requireText(moduleId, "moduleId");
        algorithmId = requireText(algorithmId, "algorithmId");
        inputFingerprint = requireText(inputFingerprint, "inputFingerprint");
        result = Objects.requireNonNull(result, "result");
        recording = Objects.requireNonNull(recording, "recording");
        if (!algorithmId.equals(recording.algorithmId())) {
            throw new IllegalArgumentException("Record algorithm ID must match its recording");
        }
        if (visualFrameCount < 0L || visualFrameCount > recording.statistics().totalEventCount()) {
            throw new IllegalArgumentException("visualFrameCount must be between zero and totalEventCount");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

}
