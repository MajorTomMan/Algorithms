package com.majortom.algorithms.telemetry.timeline;

import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import java.util.Objects;

/**
 * Immutable pairing of original execution anchors with one telemetry profile.
 *
 * <p>This is factual data. Playback speed, pause state, JavaFX animation duration, and scrub state
 * are intentionally absent; those belong only to PresentationCursor.</p>
 */
public record TelemetryTimeline(
    ExecutionAnchorTimeline execution,
    TelemetryProfile telemetry) {

  public TelemetryTimeline {
    execution = Objects.requireNonNull(execution, "execution");
    telemetry = Objects.requireNonNull(telemetry, "telemetry");
  }

  public TelemetryProjection project(PresentationCursor cursor) {
    return TimelineProjector.project(execution, telemetry.samples(), cursor);
  }
}
