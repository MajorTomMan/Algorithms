
import Basic.Structure.Maze;


public class 迷宫生成 extends Example{
    public static void main(String[] args) {
        Maze maze=new Maze(6,6);
        maze.generateMaze(0, 0);
        printGraph(maze.getMaze());
    }
}
