package com.majortom.algorithms.core.memory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Identity for one profiled execution scope. */
public record MemorySessionId(long sequence, MemoryDomain domain, String scopeId)
    implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public MemorySessionId {
    if (sequence < 0L) {
      throw new IllegalArgumentException("sequence must not be negative");
    }
    domain = Objects.requireNonNull(domain, "domain");
    scopeId = Objects.requireNonNull(scopeId, "scopeId").trim();
    if (scopeId.isEmpty()) {
      throw new IllegalArgumentException("scopeId must not be blank");
    }
  }
}
