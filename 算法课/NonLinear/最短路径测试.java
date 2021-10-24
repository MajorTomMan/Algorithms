package NonLinear;

import Func.Input_Output.In;
import Func.Input_Output.StdOut;
import Graph.DijkstraSP;
import Graph.DirectedEdge;
import Graph.EdgeWeightedDigraph;

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
    }
}
