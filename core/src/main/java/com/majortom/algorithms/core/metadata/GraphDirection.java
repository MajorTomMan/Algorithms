package com.majortom.algorithms.core.metadata;

/** Whether a graph edge can be traversed in one or both directions. */
public enum GraphDirection {
  DIRECTED,
  UNDIRECTED;

  public boolean isDirected() {
    return this == DIRECTED;
  }
}
