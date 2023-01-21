package NonLinear;


import javax.swing.JFrame;

import Basic.Structure.Maze;

public class 迷宫生成算法测试{
    public static void main(String[] args) {
        Maze maze = new Maze(11, 11);
        JFrame frame = new JFrame();
        maze.generatorMaze(2, 3);
        frame.setSize(1024, 1920);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(maze);
        frame.setVisible(true);
    }
}
