package nonlinear;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import graph.symbol.SymbolGraph;
import graph.undig.Graph;

public class 符号图测试 {
    public static void main(String[] args) throws IOException {
        testGraph_codingByMySelf();
    }

    private static void firm() throws FileNotFoundException {
        int i = 0;
        String temp = "";
        String name = "App\\Nonlinear\\data\\movies.txt";
        SymbolGraph sg = new SymbolGraph(name, "/");
        Graph graph = sg.G();
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
            if (temp.equals("N") || temp.equals("n")) {
                break;
            } else {
                System.out.print("请输入电影名字来查找演员名字(按Q退出):");
            }
        }
        System.out.println("Bye!");
        scanner.close();
    }

    private void airPort() throws FileNotFoundException {
        int i = 0;
        String temp = "";
        String name = "App\\Nonlinear\\data\\routes.txt";
        SymbolGraph sg = new SymbolGraph(name, " ");
        Graph graph = sg.G();
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
            if (temp.equals("N") || temp.equals("n")) {
                break;
            } else {
                System.out.print("请输入机场代码来查找该机场能够到达的城市(按Q退出):");
            }
        }
        System.out.println("Bye!");
        scanner.close();
    }

    private static void testGraph_codingByMySelf() {
        String[][] edges = {
                { "linkin Park", "deep Purple" },
                { "linkin Park", "led Zepplin" },
                { "deep Purple", "linkin Park" },
                { "deep Purple", "black Sabbath" },
                { "black Sabbath", "deep Purple" },
                { "black Sabbath", "led Zepplin" },
                { "led Zepplin", "black Sabbath" },
                { "led Zepplin", "linkin Park" },
        };
        basic.structure.SymbolGraph graph = new basic.structure.SymbolGraph(edges);
        graph.bfs("led Zepplin");

    }
}                          
