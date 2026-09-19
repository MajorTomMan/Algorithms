package com.majortom.algorithms.core.event.algorithm;

import com.majortom.algorithms.core.domain.observation.TreeObservationDomains;

/** Logical subtree decisions; tree topology is owned by TreeStructureEvent. */
public interface TreeAlgorithmEvent extends AlgorithmEvent {
  record SubtreeEntered(long nodeId) implements TreeAlgorithmEvent, AlgorithmEvent.Targeted {
    public SubtreeEntered { if (nodeId <= 0) throw new IllegalArgumentException("nodeId"); }
    @Override public AlgorithmEvent.EntityRef target() {
      return new AlgorithmEvent.EntityRef(TreeObservationDomains.NODE, nodeId);
    }
  }
  record SubtreeCompleted(long nodeId) implements TreeAlgorithmEvent, AlgorithmEvent.Targeted {
    public SubtreeCompleted { if (nodeId <= 0) throw new IllegalArgumentException("nodeId"); }
    @Override public AlgorithmEvent.EntityRef target() {
      return new AlgorithmEvent.EntityRef(TreeObservationDomains.NODE, nodeId);
    }
  }
}
