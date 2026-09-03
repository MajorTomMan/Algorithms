package com.majortom.algorithms.library.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;



public class SymbolGraph {
    private Map<java.lang.String, List<java.lang.String>> graph;
    private Map<java.lang.String, Boolean> visited;
    private List<java.lang.String> keys = new ArrayList<>();

    public SymbolGraph(java.lang.String[][] edges) {
        graph = new HashMap<>();
        visited = new HashMap<>();
        for (int index = 0; index < edges.length; index++) {
            java.lang.String u = edges[index][0];
            graph.put(u, new ArrayList<>());
            visited.put(u, false);
        }
        for (java.lang.String[] edge : edges) {
            java.lang.String u = edge[0];
            for (java.lang.String v : edge) {
                if (v.equals(u)) {
                    continue;
                }
                if (graph.get(u).contains(v)) {
                    continue;
                }
                graph.get(u).add(v);
            }
        }
        waking();
    }

    public List<java.lang.String> getEdges(java.lang.String vertex) {
        return graph.get(vertex);
    }

    private void waking() {
        for (Entry<java.lang.String, Boolean> data : visited.entrySet()) {
            if (!keys.contains(data.getKey())) {
                keys.add(data.getKey()); 
            }
        }
    }

    public void bfs(java.lang.String vertex) {
        visited.put(vertex, true);
        LinkedList<java.lang.String> queue = new LinkedList<>();
        queue.enqueue(vertex);
        while (!queue.isEmpty()) {
            java.lang.String v = queue.dequeue();
            System.out.println("point: "+keys.get(0) + "\t" + keys.get(1) + "\t" + keys.get(2) + "\t" + keys.get(3) + "\t");
            System.out.println("point is visited?"+"\t"+visited.get(keys.get(0)) + "\t" + visited.get(keys.get(1)) + "\t" + visited.get(keys.get(2)) + "\t" + visited.get(keys.get(3)) + " ");
            for (java.lang.String w : graph.get(v)) {
                if (!visited.get(w)) {
                    queue.enqueue(w);
                    visited.put(w, true);
                } 
            }
        }
    }
}
