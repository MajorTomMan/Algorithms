package NonLinear;


import javax.swing.JFrame;

import basic.structure.Maze;

public class 迷宫生成算法测试{
    public static void main(String[] args) {
        Maze maze = new Maze(11, 11);
        maze.printMap();
        JFrame frame = new JFrame();
        maze.generatorMaze(2, 3);
/*         frame.setSize(1024, 1920);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(maze);
        frame.setVisible(true); */
    }
}
