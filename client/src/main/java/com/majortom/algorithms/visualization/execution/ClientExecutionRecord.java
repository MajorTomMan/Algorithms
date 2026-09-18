package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.runtime.ExecutionAnchorTimeline;
import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import java.util.Objects;

/**
 * Client history entry that decorates the shared recording with presentation identity and frame
 * statistics.
 */
public record ClientExecutionRecord(String moduleId, String operationId, String inputFingerprint,
    ExecutionResult result, ExecutionRecording recording,
    ExecutionAnchorTimeline executionAnchors, long visualFrameCount) {
  public ClientExecutionRecord {
    moduleId = requireText(moduleId, "moduleId");
    operationId = requireText(operationId, "operationId");
    inputFingerprint = requireText(inputFingerprint, "inputFingerprint");
    result = Objects.requireNonNull(result, "result");
    recording = Objects.requireNonNull(recording, "recording");
    executionAnchors = Objects.requireNonNull(executionAnchors, "executionAnchors");
    if (!operationId.equals(recording.operationId())) {
      throw new IllegalArgumentException("Record operation ID must match its recording");
    }
    if (!recording.runId().equals(executionAnchors.runId())) {
      throw new IllegalArgumentException("Execution anchors must belong to the recorded run");
    }
    if (executionAnchors.anchors().size() != recording.events().size()) {
      throw new IllegalArgumentException("Execution anchors must cover every recorded event");
    }
    for (int index = 0; index < recording.events().size(); index++) {
      if (executionAnchors.anchors().get(index).eventSequence()
          != recording.events().get(index).sequence()) {
        throw new IllegalArgumentException("Execution anchor sequence must match recorded events");
      }
    }
    if (visualFrameCount < 0L || visualFrameCount > recording.statistics().totalEventCount()) {
      throw new IllegalArgumentException(
          "visualFrameCount must be between zero and totalEventCount");
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
