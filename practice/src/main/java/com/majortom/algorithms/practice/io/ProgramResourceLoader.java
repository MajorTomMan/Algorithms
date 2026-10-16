package com.majortom.algorithms.practice.io;

import com.majortom.algorithms.core.io.AbstractJsonResourceLoader;

/**
 * Program/practice module resource loader.
 *
 * <p>Uses the same core loader contract, but anchors resource lookup
 * to the practice module rather than an algorithm implementation.
 */
public final class ProgramResourceLoader extends AbstractJsonResourceLoader {
  public ProgramResourceLoader() {
    super(ProgramResourceLoader.class);
  }
}
