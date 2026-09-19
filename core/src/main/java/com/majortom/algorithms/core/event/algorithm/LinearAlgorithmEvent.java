package com.majortom.algorithms.core.event.algorithm;

/** Algorithm decisions for stacks/queues; push/pop are exclusively structure operations. */
public interface LinearAlgorithmEvent extends AlgorithmEvent {
  record ElementConsidered(String source, int index)
      implements LinearAlgorithmEvent, AlgorithmEvent.Targeted {
    public ElementConsidered {
      if (source == null || source.isBlank()) throw new IllegalArgumentException("source");
      if (index < 0) throw new IllegalArgumentException("index");
    }
    @Override public AlgorithmEvent.IndexRef target() {
      return new AlgorithmEvent.IndexRef(source, index);
    }
  }
}
