package com.majortom.algorithms.app.leetcode.ds.graph;

import com.majortom.algorithms.library.basic.graph.Graph;
import com.majortom.algorithms.library.basic.graph.Vertex;
import edu.princeton.cs.algs4.SymbolGraph;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

public class 符号图测试 {
    public static void main(String[] args) throws IOException {
        testCanonicalGraph();
    }

    private static void firm() throws FileNotFoundException {
        int i = 0;
        String temp;
        String name = "App\\Nonlinear\\data\\movies.txt";
        SymbolGraph sg = new SymbolGraph(name, "/");
        edu.princeton.cs.algs4.Graph graph = sg.G();
        System.out.print("请输入电影名字来查找演员名字(按Q退出):");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String[] temparr = scanner.nextLine().split("/");
            System.out.print("参演演员有:");
            for (int w : graph.adj(sg.index(temparr[i]))) {
                System.out.print(sg.name(w) + " ");
            }
            System.out.println();
            System.out.println("请问是否继续查找?(Y/N):");
            temp = scanner.nextLine();
            if (temp.equalsIgnoreCase("N")) {
                break;
            }
            System.out.print("请输入电影名字来查找演员名字(按Q退出):");
        }
        System.out.println("Bye!");
        scanner.close();
    }

    private void airPort() throws FileNotFoundException {
        int i = 0;
        String temp;
        String name = "App\\Nonlinear\\data\\routes.txt";
        SymbolGraph sg = new SymbolGraph(name, " ");
        edu.princeton.cs.algs4.Graph graph = sg.G();
        System.out.print("请输入机场代码来查找该机场能够到达的城市(按Q退出):");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String[] temparr = scanner.nextLine().split(" ");
            System.out.print("能够到达的城市有:");
            for (int w : graph.adj(sg.index(temparr[i]))) {
                System.out.print(sg.name(w) + " ");
            }
            System.out.println();
            System.out.println("请问是否继续查找?(Y/N):");
            temp = scanner.nextLine();
            if (temp.equalsIgnoreCase("N")) {
                break;
            }
            System.out.print("请输入机场代码来查找该机场能够到达的城市(按Q退出):");
        }
        System.out.println("Bye!");
        scanner.close();
    }

    private static void testCanonicalGraph() {
        String[][] edges = {
                {"linkin Park", "deep Purple"},
                {"linkin Park", "led Zepplin"},
                {"deep Purple", "black Sabbath"},
                {"black Sabbath", "led Zepplin"}
        };
        Graph<String> graph = new Graph<>();
        for (String[] edge : edges) {
            Vertex<String> from = graph.vertex(edge[0]);
            if (from == null) {
                from = graph.addVertex(edge[0]);
            }
            Vertex<String> to = graph.vertex(edge[1]);
            if (to == null) {
                to = graph.addVertex(edge[1]);
            }
            graph.addEdge(from, to);
        }
        bfs(graph, graph.vertex("led Zepplin"));
    }

    private static void bfs(Graph<String> graph, Vertex<String> start) {
        Queue<Vertex<String>> queue = new ArrayDeque<>();
        Set<Vertex<String>> visited = new LinkedHashSet<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            Vertex<String> current = queue.remove();
            System.out.println(current.value());
            for (Vertex<String> neighbor : graph.neighbors(current)) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
    }
}
