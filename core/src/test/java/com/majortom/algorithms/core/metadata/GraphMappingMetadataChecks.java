package com.majortom.algorithms.core.metadata;

import com.majortom.algorithms.core.annotation.GraphEdges;
import com.majortom.algorithms.core.annotation.GraphNodes;
import java.util.List;

/** Standalone core checks: graph classes are intentionally not imported. */
public final class GraphMappingMetadataChecks {
  record Link(String from, String to, int distance, boolean twoWay, int alternateCost) {}
  record Network(@GraphNodes List<String> cities,
      @GraphEdges(from="from", to="to", weight="distance", bidirectional="twoWay")
      List<Link> roads,
      @GraphEdges(from="to", to="from", weight="alternateCost")
      List<Link> detours) {}

  static final class GetterLink {
    String from() { return "A"; }
    String to() { return "B"; }
    int getDistance() { return 8; }
    boolean isTwoWay() { return true; }
  }
  record GetterNetwork(@GraphEdges(from="from", to="to", weight="distance",
      bidirectional="twoWay") List<GetterLink> roads) {}

  public static void main(String[] args) {
    var metadata = GraphMappingMetadata.describe(Network.class);
    check(metadata == GraphMappingMetadata.describe(Network.class), "cached container metadata");
    check(metadata.nodes().size() == 1 && metadata.edges().size() == 2,
        "record annotations deduplicated");
    Link link = new Link("A", "B", 7, true, 12);
    var forward = metadata.edges().get(0).read(link);
    var reverse = metadata.edges().get(1).read(link);
    check(forward.from().equals("A") && forward.to().equals("B")
        && forward.weight() == 7 && forward.bidirectional(), "forward edge values");
    check(reverse.from().equals("B") && reverse.to().equals("A")
        && reverse.weight() == 12 && !reverse.bidirectional(), "same type different mapping");

    var getter = GraphMappingMetadata.describe(GetterNetwork.class).edges().getFirst()
        .read(new GetterLink());
    check(getter.weight() == 8 && getter.bidirectional(), "getter edge properties");

    record DefaultLink(String from, String to) {}
    record DefaultNetwork(@GraphEdges List<DefaultLink> edges) {}
    var defaults = GraphMappingMetadata.describe(DefaultNetwork.class).edges().getFirst()
        .read(new DefaultLink("A", "A"));
    check(defaults.weight() == 1.0 && !defaults.bidirectional()
        && defaults.from().equals(defaults.to()), "defaults and self-loop values");

    record InvalidLink(String from, String to, String weight) {}
    record InvalidNetwork(@GraphEdges(weight="weight") List<InvalidLink> edges) {}
    boolean rejected = false;
    try {
      GraphMappingMetadata.describe(InvalidNetwork.class).edges().getFirst()
          .read(new InvalidLink("A", "B", "wrong"));
    } catch (IllegalArgumentException expected) { rejected = true; }
    check(rejected, "invalid edge weight");
    System.out.println("GraphMappingMetadataChecks passed");
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
