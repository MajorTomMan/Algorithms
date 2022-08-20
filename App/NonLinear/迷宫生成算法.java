package NonLinear;

import java.util.Random;

public class 迷宫生成算法 extends Example{
    private static int[] start=new int[2];
    private static int[] destination=new int[2];
    public static void main(String[] args) {
        int[][] map=createMap(new int[5][5]);
        randomStart();
        randomDestination();
        map[start[0]][start[1]]=4;
        map[destination[0]][destination[1]]=5;
        randomBFS(start[0], start[1], new boolean[5][5],map);
    }
    private static void randomBFS(int x,int y,boolean[][] visited,int[][] graph){
        if(x==graph.length||x<0){
            return;
        }
        else if(y==graph[x].length||y<0){
            return;
        }
        else if(visited[x][y]==true){
            return;
        }
        randomWay(x,y,visited,graph);
        if(x==0){
            if(!visited[x+1][y]||!visited[x][y+1]||!visited[x][y-1]){
                randomWay(x, y, visited, graph);
            }
        }
        else if(y==0){
            if(visited[x-1][y]&&visited[x+1][y]&&visited[x][y+1]){
                randomWay(x, y, visited, graph);
            }
        }
        else if(visited[x-1][y]&&visited[x][y-1]&&visited[x+1][y]&&visited[x][y+1]){
            return;
        }
        else if(x==destination[0]&&y==destination[1]){
            System.out.println("找到了终点");
            return;
        }
        else{
            randomWay(x, y, visited, graph);
        }
    }
    private static void randomWay(int x,int y,boolean[][] visited,int[][] graph){
        visited[x][y]=true;
        if(x==start[0]&&y==start[1]){

        }
        else if(x==destination[0]&&y==destination[1]){

        }
        else{
            graph[x][y]=0;
        }
        printGraph(graph, visited);
        int whichWay=new Random().nextInt(4)+1;
        System.out.printf("Random Number is:%d\n",whichWay);
        switch(whichWay){ //上左下右
            case 1: 
                randomBFS(x-1, y, visited, graph);
                break;
            case 2: 
                randomBFS(x, y-1, visited, graph);
                break;
            case 3: 
                randomBFS(x+1, y, visited, graph);
                break;
            case 4: 
                randomBFS(x, y+1, visited, graph);
                break;
        }
    }
    private static int[][] createMap(int[][] map){
        for(int i=0;i<map.length;i++){
            for(int j=0;j<map[0].length;j++){
                map[i][j]=1;
            }
        }
        return map;
    }
    private static void randomStart(){
        int x=new Random().nextInt(4)+1;
        int y=new Random().nextInt(4)+1;
        start[0]=x;
        start[1]=y;
    }
    private static void randomDestination(){
        int x=new Random().nextInt(4)+1;
        int y=new Random().nextInt(4)+1;
        destination[0]=x;
        destination[1]=y;
    }
}
