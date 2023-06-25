package nonlinear;

import basic.structure.Graph;

public class 并查集测试 {
    public static void main(String[] args) {
        Graph graph = new Graph(6, new int[][] {
                { 0, 4 },
                { 1, 1 }, { 1, 5 },
                { 2, 2 }, { 2, 3 }, { 2, 5 },
                { 3, 2 }, { 3, 3 },
                { 4, 4 },
                { 5, 5 },
        });
        System.out.println(graph.hasCycle());
        System.out.println(graph.isConnected(0, 5));
    }
}
