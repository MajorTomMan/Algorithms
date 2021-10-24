package Graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import Search.Structure.RedBlackBST;

public class SymboGraph {
    private RedBlackBST<String,Integer> st;
    private String[] keys;
    private Graph G;
    public SymboGraph(String stream,String sp) throws FileNotFoundException {
        st=new RedBlackBST<>();
        Scanner scanner=new Scanner(new FileReader(new File(stream)));
        while(scanner.hasNextLine()){
            String[] a=scanner.nextLine().split(sp);
            for(int i=0;i<a.length;i++){
                if(!st.contains(a[i])){
                    st.put(a[i],st.size());
                }
            }
        }
        keys=new String[st.size()];
        for(String name:st.keys()){
            keys[st.get(name)]=name;
        }
        scanner.close();
        G=new Graph(st.size());
        scanner=new Scanner(new FileReader(new File(stream)));
        while(scanner.hasNextLine()){
            String[] a=scanner.nextLine().split(sp);
            int v=st.get(a[0]);
            for(int i=1;i<a.length;i++){
                G.addEdge(v,st.get(a[i]));
            }
        }
        scanner.close();
    }
    public boolean contains(String s){
        return st.contains(s);
    }
    public int index(String s){
        System.out.println();
        return st.get(s);
    }
    public String name(int v){
        return keys[v];
    }
    public Graph G(){
        return G;
    }
}
