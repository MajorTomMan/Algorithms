package nonlinear;

import func.io.In;
import func.io.StdOut;
import graph.AcyclicSP;
import graph.DijkstraSP;
import graph.dir.DirectedEdge;
import graph.dir.EdgeWeightedDigraph;

public class 最短路径测试 {
    public static void main(String[] args) {
        String fileaddr="NonLinear\\data\\tinyEWD.txt";
        EdgeWeightedDigraph G=new EdgeWeightedDigraph(new In(fileaddr));
        int s=0;
        DijkstraSP sp=new DijkstraSP(G, s);
        for (int t = 0; t < G.V(); t++) {
            if (sp.hasPathTo(t)) {
                StdOut.printf("%d to %d (%.2f)  ", s, t, sp.distTo(t));
                for (DirectedEdge e : sp.pathTo(t)) {
                    StdOut.print(e + "   ");
                }
                StdOut.println();
            }
            else {
                StdOut.printf("%d to %d         no path\n", s, t);
            }
        }
        System.out.println("---------------------------------------");
        AcyclicSP(fileaddr,G);
    }
    private static void AcyclicSP(String fileaddr,EdgeWeightedDigraph G){ //无环加权有向图的最短路径
        AcyclicSP ASP=new AcyclicSP(G,0);
        for (int t = 0; t < G.V(); t++) {
            if (ASP.hasPathTo(t)) {
                StdOut.printf("%d to %d (%.2f)  ", 0, t, ASP.distTo(t));
                for (DirectedEdge e : ASP.pathTo(t)) {
                    StdOut.print(e + "   ");
                }
                StdOut.println();
            }
            else {
                StdOut.printf("%d to %d         no path\n", 0, t);
            }
        }
    }
}
