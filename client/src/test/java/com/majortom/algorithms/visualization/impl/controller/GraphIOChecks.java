package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.annotation.*;
import com.majortom.algorithms.core.io.FileDataIO;
import com.majortom.algorithms.core.io.JsonDataCodec;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.GraphStructure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Headless check: the existing core.io pipeline supplies typed objects to Graph. */
public final class GraphIOChecks {
  @ValueType(GraphStructure.class)
  public record City(@ValueId String code, @ValueField(ValueDisplay.SUMMARY) String name) {}
  public record Railway(City from, City to, double kilometers, String line) {}
  public record Road(City from, City to, boolean twoWay, String name) {}
  public record Network(@GraphNodes List<City> cities,
      @GraphEdges(from="from", to="to", weight="kilometers") List<Railway> railways,
      @GraphEdges(from="from", to="to", bidirectional="twoWay") List<Road> roads) {}

  public static void main(String[] args) throws Exception {
    String json = """
      {
        "cities": [{"code":"sz","name":"深圳"},
                   {"code":"gz","name":"广州"},
                   {"code":"dg","name":"东莞"}],
        "railways": [{"from":{"code":"sz","name":"深圳"},
                      "to":{"code":"gz","name":"广州"},
                      "kilometers":102,"line":"广深铁路"}],
        "roads": [{"from":{"code":"sz","name":"深圳"},
                   "to":{"code":"gz","name":"广州"},
                   "twoWay":true,"name":"广深高速"}]
      }
      """;
    JsonDataCodec<Network> codec = new JsonDataCodec<>(Network.class);
    Network network = codec.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    if (network.cities().size() != 3) throw new AssertionError("JSON record decoding");

    Path file = Files.createTempFile("multi-graph-io", ".json");
    try {
      FileDataIO.write(file, network, codec);
      network = FileDataIO.read(file, codec);
    } finally {
      Files.deleteIfExists(file);
    }
    Graph<City> graph = Graph.from(GraphDirection.DIRECTED, City.class, network);
    if (graph.vertexCount() != 3 || graph.edgeCount() != 3)
      throw new AssertionError("JSON graph topology");
    var vertices = graph.snapshot().vertices();
    var sz = graph.vertex(vertices.get(0).id());
    var gz = graph.vertex(vertices.get(1).id());
    var dg = graph.vertex(vertices.get(2).id());
    if (graph.edges(sz, gz).size() != 2 || graph.edges(gz, sz).size() != 1
        || !graph.outgoingEdges(dg).isEmpty())
      throw new AssertionError("parallel/bidirectional edges or isolated node");

    boolean rejected = false;
    try {
      codec.read(new ByteArrayInputStream("{bad-json}".getBytes(StandardCharsets.UTF_8)));
    } catch (IOException expected) { rejected = true; }
    if (!rejected) throw new AssertionError("malformed JSON must fail");
    System.out.println("GraphIOChecks passed");
  }
}