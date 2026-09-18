package com.majortom.algorithms.core.memory;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Optional JFR allocation breakdown for one execution scope. Values are sampled estimates, not exact allocation totals. */
public record MemoryDeepProfile(
    MemoryDomain domain,
    String scopeId,
    long estimatedBytes,
    long sampleCount,
    List<MemoryAllocationTypeStat> topTypes,
    List<MemoryAllocationSiteStat> topSites)
    implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public MemoryDeepProfile {
    domain = Objects.requireNonNull(domain, "domain");
    scopeId = Objects.requireNonNullElse(scopeId, "");
    estimatedBytes = Math.max(0L, estimatedBytes);
    sampleCount = Math.max(0L, sampleCount);
    topTypes = topTypes == null ? List.of() : List.copyOf(topTypes);
    topSites = topSites == null ? List.of() : List.copyOf(topSites);
  }

  public boolean hasSamples() {
    return sampleCount > 0L;
  }
}
