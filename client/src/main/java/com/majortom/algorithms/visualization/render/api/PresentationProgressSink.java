package com.majortom.algorithms.visualization.render.api;

/** Non-blocking bridge used by FX animation playback to advance the current presentation cursor. */
@FunctionalInterface
public interface PresentationProgressSink {
  PresentationProgressSink NONE = progress -> {};

  void publish(double progress);
}
