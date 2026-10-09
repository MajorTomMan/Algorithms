package com.majortom.algorithms.structure.maze;

/** Describes how a generated array maze chooses its entrance and exit. */
public enum MazeEndpointMode {
  /** Choose two logical maze cells at random. */
  RANDOM,
  /** Use the first and last logical maze cells. */
  FIXED,
  /** Use the coordinates supplied by the caller. */
  CUSTOM
}
