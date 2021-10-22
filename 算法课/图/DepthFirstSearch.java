package 图;

public class DepthFirstSearch {
    private boolean[] marked;
    private int count;
    public DepthFirstSearch(Graph G,int s){
        marked=new boolean[G.V()];
        dfs(G,s);
    }
    private void dfs(Graph G, int v) {
        marked[v]=true;
        count++;
        for (int w : G.adj(v)) {
            if(!marked[w]){
                dfs(G,w);
            }
        }
    }
    private boolean marked(int w){
        return marked[w];
    }
    public int count(){
        return count;
    }
}
