package com.majortom.algorithms.visualization.render.runtime;

/** Named pipeline stages used by trace output and invariant tests. */
public enum RenderPipeline {
  CAPTURE,
  DESCRIBE,
  WAIT_LAYOUT,
  RESOLVE_LAYOUT,
  WAIT_APPLY,
  WAIT_PULSE,
  CAMERA,
  COMMIT,
  PRESENTED
}
