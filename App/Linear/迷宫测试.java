
import basic.structure.Maze;

public class 迷宫测试 {
    public static void main(String[] args) {
        Maze maze=new Maze(40,40); 
        maze.print();
        System.out.println("--------------Random Recursion-----------");
        maze.generatorMap();
        maze.print();
    }
}
