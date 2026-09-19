package com.majortom.algorithms.core.event.algorithm;

/** Logical algorithm cursor movement; changing next/previous remains a StructureEvent. */
public interface LinkedAlgorithmEvent extends AlgorithmEvent {
  record CursorMoved(String cursorName, long nodeId)
      implements LinkedAlgorithmEvent, AlgorithmEvent.Targeted {
    public CursorMoved {
      if (cursorName == null || cursorName.isBlank()) throw new IllegalArgumentException("cursorName");
      if (nodeId <= 0) throw new IllegalArgumentException("nodeId");
    }
    @Override public AlgorithmEvent.EntityRef target() {
      return new AlgorithmEvent.EntityRef("linked.node", nodeId);
    }
  }
}
