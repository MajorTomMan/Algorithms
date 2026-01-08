package com.majortom.algorithms.app.leetcode.nonlinear;


import com.majortom.algorithms.core.basic.Graph;
import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Queue;

public class 多余连接 {
    public static void main(String[] args) {
        Integer[][] edges={
            {0,1},{1,2},{2,3},{3,1}
        };
        Graph graph=new Graph(4,edges);
        System.out.println(bfs(1,graph.getVisited(),graph));
    }
    private static boolean bfs(int v,boolean[] visited,Graph graph){
        visited[v]=true;
        Queue<Integer> queue = new LinkedList<>();
        queue.add(v);
        while(!queue.isEmpty()){
            int vertex=queue.poll();
            for(int w:graph.getGraph().get(vertex)){
                if(!visited[w]){
                    queue.add(w);
                    visited[w]=true;
                }
            }
        }
        return false;
    }
}
