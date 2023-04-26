package basic.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SymbolGraph {
    private Map<String, List<String>> graph;
    private Map<String, Boolean> visited;
    private List<String> keys = new ArrayList<>();

    public SymbolGraph(String[][] edges) {
        graph = new HashMap<>();
        visited = new HashMap<>();
        for (int index = 0; index < edges.length; index++) {
            String u = edges[index][0];
            graph.put(u, new ArrayList<>());
            visited.put(u, false);
        }
        for (String[] edge : edges) {
            String u = edge[0];
            for (String v : edge) {
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

    public List<String> getEdges(String vertex) {
        return graph.get(vertex);
    }

    private void waking() {
        for (Entry<String, Boolean> data : visited.entrySet()) {
            if (!keys.contains(data.getKey())) {
                keys.add(data.getKey()); 
            }
        }
    }

    public void bfs(String vertex) {
        visited.put(vertex, true);
        Queue<String> queue = new Queue<>();
        queue.enqueue(vertex);
        while (!queue.isEmpty()) {
            String v = queue.dequeue();
            System.out.println("point: "+keys.get(0) + "\t" + keys.get(1) + "\t" + keys.get(2) + "\t" + keys.get(3) + "\t");
            System.out.println("point is visited?"+"\t"+visited.get(keys.get(0)) + "\t" + visited.get(keys.get(1)) + "\t" + visited.get(keys.get(2)) + "\t" + visited.get(keys.get(3)) + " ");
            for (String w : graph.get(v)) {
                if (!visited.get(w)) {
                    queue.enqueue(w);
                    visited.put(w, true);
                } 
            }
        }
    }
}