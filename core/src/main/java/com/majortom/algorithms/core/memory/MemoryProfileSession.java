package com.majortom.algorithms.core.memory;

/** Mutable lifetime boundary for exactly one profiled execution scope. */
public interface MemoryProfileSession extends AutoCloseable {
  MemorySessionId id();

  MemoryCapabilities capabilities();

  /** Adds the calling platform thread to this scope. Returns false when attribution is unavailable. */
  boolean attachCurrentThread();

  /** Returns a point-in-time snapshot; it remains valid after this session changes or closes. */
  MemoryProfile snapshot();

  @Override
  void close();
}
