package basic.structure.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Time;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import basic.structure.Maze;

public class MazeVisualization extends JPanel {
    private int[][] map;
    private int width;
    private int height;
    private int cell_size = 20;
    private Color wall_color = Color.BLACK;
    private Color path_color = Color.WHITE;
    private Color start_color = Color.GREEN;
    private Color end_color = Color.RED;
    private Color currentPoint_color = Color.BLUE;
    private int[] currentPoint = new int[2];
    private int delay = 100;
    private Timer timer;

    public MazeVisualization(int[][] map) {
        this.map = map;
        // Initialize width and height based on map dimensions and cell_size
        width = map[0].length * cell_size;
        height = map.length * cell_size;

        setPreferredSize(new Dimension(width, height));

        JFrame frame = new JFrame("地图生成");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.getContentPane().add(this);
        frame.setVisible(true);

        // Initialize the timer
        timer = new Timer(delay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMaze(map);
                repaint();
            }
        });
        // Start the timer
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // TODO Auto-generated method stub
        super.paintComponent(g);
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                int cell = map[i][j];
                int x = j * cell_size;
                int y = i * cell_size;
                if (cell == 1) {
                    g.setColor(wall_color);
                    g.fillRect(x, y, cell_size, cell_size);
                } else if (cell == 0) {
                    g.setColor(path_color);
                    g.fillRect(x, y, cell_size, cell_size);
                } else if (cell == 2) {
                    g.setColor(start_color);
                    g.fillRect(x, y, cell_size, cell_size);
                } else if (cell == 3) {
                    g.setColor(end_color);
                    g.fillRect(x, y, cell_size, cell_size);
                }
            }
        }
    }

    public void setCurrentPoint() {

    }

    public void updateMaze(int[][] map) {
        this.map = map;
        repaint();
    }
}
