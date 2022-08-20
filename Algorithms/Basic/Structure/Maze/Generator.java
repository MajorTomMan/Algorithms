package Basic.Structure.Maze;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Generator extends JFrame {
    private int destination = 9;
    private int startPoint=3;
    private int wall = 1;
    private int pathIndex;
    private int[][] maze = {
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 0, 1, 0, 0, 0, 0, 0, 1 },
            { 1, 1, 0, 1, 1, 0, 1, 0, 1, 1 },
            { 1, 1, 0, 1, 0, 0, 1, 0, 1, 1 },
            { 1, 1, 0, 1, 0, 0, 0, 0, 0, 1 },
            { 1, 1, 0, 1, 1, 1, 0, 1, 1, 1 },
            { 1, 1, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
    };
    private List<Integer> path = new ArrayList<>();

    private void mazeGeneator(int totalRow, int totalCol) {
        maze = new int[totalRow][totalCol];
        for (int row = 0; row < totalRow; row++) {
            for (int col = 0; col < totalCol; col++) {
                if (row == 0 || row == totalRow - 1 || col == 0 || col == totalCol - 1) {
                    maze[row][col] = wall;
                    continue;
                }
                maze[row][col] = new Random().nextInt(2);
            }
        }
    }

    private void displayMaze() {
        for (int row = 0; row < maze.length; row++) {
            for (int col = 0; col < maze[0].length; col++) {
                System.out.print(maze[row][col] + "  ");
            }
            System.out.println();
        }
    }

    private void setDestination(int row, int col) {
        maze[row][col] = destination;
    }
    private void setStartPoint(int row, int col) {
        maze[row][col] = startPoint;
    }

    public Generator() {
        displayMaze();
        setDestination(maze.length-2, maze[0].length-2);
        setStartPoint(1,1);
        setTitle("a little maze generator");
        setSize(640, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        DFS.searchPath(maze, 1, 1, path);
        pathIndex = path.size() - 2;
        System.out.println(path);
    }
    @Override
    public void paint(Graphics g) {
        // TODO Auto-generated method stub
        super.paint(g);
        for (int row = 0; row < maze.length; row++) {
            for (int col = 0; col < maze[0].length; col++) {
                Color color;
                switch (maze[row][col]) {
                    case 1:
                        color = Color.BLACK;
                        break;
                    case 9:
                        color = Color.RED;
                        break;
                    default:
                        color = Color.WHITE;
                        break;
                }
                g.setColor(color);
                g.fillRect(30 * col, 30 * row, 30, 30);
                g.setColor(Color.BLACK);
                g.drawRect(30 * col, 30 * row, 30, 30);
            }
        }
        for (int p = 0; p < path.size(); p += 2) {
            int pathX = path.get(p);
            int pathY = path.get(p + 1);
            if(maze[pathY][pathX]==destination){
            }
            else if(maze[pathY][pathX]==startPoint){
                g.setColor(Color.PINK);
                g.fillRect(pathX * 30, pathY * 30, 30, 30);
            }
            else{
                g.setColor(Color.GREEN);
                g.fillRect(pathX * 30, pathY * 30, 30, 30);
            }
        }
        int pathX=path.get(pathIndex);
        int pathY=path.get(pathIndex+1);
        g.setColor(Color.ORANGE);
        g.fillOval(pathX * 30, pathY * 30, 30, 30);
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        // TODO Auto-generated method stub
        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            pathIndex -= 2;
            if (pathIndex < 0) {
                pathIndex = 0;
            }
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            pathIndex += 2;
            if (pathIndex > path.size() - 2) {
                pathIndex = path.size() - 2;
            }
        }
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Generator generator = new Generator();
                generator.setVisible(true);
            }
        });
    }
}
