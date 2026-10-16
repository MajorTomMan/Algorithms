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
/**
 * “城市”顶点类型下的最短路径算法入口，目前保留算法体占位。
 *
 * <p>本例演示声明式算法生命周期：@Algorithm 注册 City 专用算法；
 * @AlgorithmInitialize 在每次运行前读取示例路网并初始化 GraphStructure；
 * @AlgorithmEntry 才是之后实现 Dijkstra 等算法的执行入口。
 *
 * <p>两个方法由 AlgorithmDescriptor.invoke 在同一执行上下文按序调用。
 * 直接调用 execute() 不会触发初始化；手动修改这里的图也不会
 * 自动更新工作台原始编辑区，因为算法在其快照副本上运行。
 *
 * <p>当前 roads.json 使用嵌套 City JSON 对象匹配 Road.from/to，
 * 图映射再通过 City.@ValueId 关联城市，不需要额外 RoadInput 类。
 * 本类现在还没有计算或发布最短路径结果，不要把注册成功误认为算法已完成。
 */
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