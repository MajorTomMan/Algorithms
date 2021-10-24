package NonLinear;

import java.io.FileNotFoundException;

import Func.Input_Output.StdIn;
import Func.Input_Output.StdOut;
import Graph.Graph;
import Graph.SymboGraph;

public class 符号图测试{
    public static void main(String[] args) throws FileNotFoundException{
        String name="NonLinear\\data\\movies-hero.txt";
        SymboGraph sg = new SymboGraph(name,"/");
        Graph graph = sg.G();
        while (StdIn.hasNextLine()) {
            String source = StdIn.readLine();
            if (sg.contains(source)) {
                int s = sg.index(source);
                for (int v : graph.adj(s)) {
                    StdOut.println("   " + sg.name(v));
                }
            }
            else {
                StdOut.println("input not contain '" + source + "'");
            }
        }
    }
}
