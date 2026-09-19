package com.majortom.algorithms.core.event.algorithm;

import com.majortom.algorithms.core.domain.observation.GraphObservationDomains;

/** Algorithm decisions about a graph; no vertex, edge or weight is modified. */
public interface GraphAlgorithmEvent extends AlgorithmEvent {
  record EdgeConsidered(long edgeId, long fromId, long toId)
      implements GraphAlgorithmEvent, AlgorithmEvent.Targeted {
    public EdgeConsidered { check(edgeId); check(fromId); check(toId); }
    @Override public AlgorithmEvent.EntityRef target() {
      return new AlgorithmEvent.EntityRef(GraphObservationDomains.EDGE, edgeId);
    }
  }
  record EdgeAccepted(long edgeId, long fromId, long toId)
      implements GraphAlgorithmEvent, AlgorithmEvent.Targeted {
    public EdgeAccepted { check(edgeId); check(fromId); check(toId); }
    @Override public AlgorithmEvent.EntityRef target() {
      return new AlgorithmEvent.EntityRef(GraphObservationDomains.EDGE, edgeId);
    }
  }
  record EdgeRejected(long edgeId, long fromId, long toId)
      implements GraphAlgorithmEvent, AlgorithmEvent.Targeted {
    public EdgeRejected { check(edgeId); check(fromId); check(toId); }
    @Override public AlgorithmEvent.EntityRef target() {
      return new AlgorithmEvent.EntityRef(GraphObservationDomains.EDGE, edgeId);
    }
  }
  private static void check(long id) {
    if (id <= 0) throw new IllegalArgumentException("graph identifiers must be positive");
  }
}
