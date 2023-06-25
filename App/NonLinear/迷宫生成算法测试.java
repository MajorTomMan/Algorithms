package nonlinear;

import basic.structure.Maze;

public class 迷宫生成算法测试{
    public static void main(String[] args) {
        Maze maze=new Maze(50, 50);
        maze.print();
        System.out.println("------------------------------");
        maze.generatorMap();
        maze.print();
        System.out.println(maze.isConnected(true));
    }
}
