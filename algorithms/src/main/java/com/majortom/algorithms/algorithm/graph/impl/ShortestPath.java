package com.majortom.algorithms.algorithm.graph.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.majortom.algorithms.algorithm.graph.model.City;
import com.majortom.algorithms.algorithm.graph.model.Region;
import com.majortom.algorithms.algorithm.graph.model.Road;
import com.majortom.algorithms.algorithm.io.AlgorithmResourceLoader;
import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.annotation.AlgorithmInitialize;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.GraphStructure;

@Algorithm(id = "shortest-path", name = "最短路径", types = { City.class }, structure = GraphStructure.class)
public class ShortestPath {
    @AlgorithmInitialize
    public void init(GraphStructure<City> graph) {
        List<City> cities = AlgorithmResourceLoader.load(
                "graph/regions/demo/cities.json",
                new TypeReference<List<City>>() {
                });

        List<Road> roads = AlgorithmResourceLoader.load(
                "graph/regions/demo/roads.json",
                new TypeReference<List<Road>>() {
                });

        Region region = new Region("中国", cities, roads);

        Graph<City> source = Graph.from(
                graph.direction(),
                City.class,
                region);

        graph.initialize(source.snapshot());
    }

    @AlgorithmEntry
    public void execute(GraphStructure<City> graph) {
    }
}