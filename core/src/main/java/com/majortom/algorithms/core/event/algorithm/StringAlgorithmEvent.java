package com.majortom.algorithms.core.event.algorithm;

import com.majortom.algorithms.core.domain.observation.StringObservationDomains;

/** Logical string-matching alignment, never a mutation of StringStructure. */
public interface StringAlgorithmEvent extends AlgorithmEvent {
  record PatternAligned(int textIndex, int patternIndex)
      implements StringAlgorithmEvent, AlgorithmEvent.Targeted {
    public PatternAligned {
      if (textIndex < 0 || patternIndex < 0) throw new IllegalArgumentException("negative index");
    }
    @Override public AlgorithmEvent.IndexRef target() {
      return new AlgorithmEvent.IndexRef(StringObservationDomains.TARGET_SOURCE, textIndex);
    }
  }
}
