package Basic.Structure;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JPanel;

public class Maze extends JPanel {
    private int[][] map;
    private int wall = 1;
    private int road = 0;
    private int begin = 2;
    private int dest = 3;
    private int[] dx = { -1, 1, 0, 0 }; // X轴上的四个方向（上下左右)
    private int[] dy = { 0, 0, -1, 1 }; // Y轴上的四个方向(上下左右)
    private Boolean destExist = false;
    private Boolean[][] visited;
    private final int blockSize = 60;

    /**
     * @param map   地图
     * @param wall  墙
     * @param road  路
     * @param begin 起点
     * @param dest  终点
     */
    public Maze(int row, int column) {
        map = new int[row][column];
        visited = new Boolean[row][column];
        for (int i = 0; i < visited.length; i++) {
            for (int j = 0; j < visited[0].length; j++) {
                visited[i][j] = false;
            }
        }
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                map[i][j] = 1;
            }
        }
    }

    /* 生成地图 */
    public void generatorMaze(int startX, int startY) {
        map[startX][startY] = begin;
        Recursion(startX, startY, visited, 0);
    }

    private void Recursion(int y, int x, Boolean[][] visited, int depth) {
        /* 确保唯一终点会生成且只有一个 */
        if (destExist) {
            return;
        }
        /* 根据给定的递归深度来确定终点的生成时机 */
        if (depth == 30) {
            if (!destExist) {
                map[y][x] = dest;
                destExist = true;
                visited[y][x] = true;
            }
            return;
        }
        // 随机打乱四个方向的顺序
        List<Integer> dxList = Arrays.stream(dx).boxed().collect(Collectors.toList());
        List<Integer> dyList = Arrays.stream(dy).boxed().collect(Collectors.toList());
        Collections.shuffle(dxList);
        Collections.shuffle(dyList);
        dx = dxList.stream().mapToInt(i -> i).toArray();
        dy = dyList.stream().mapToInt(i -> i).toArray();
        /* 随机选择四个方向上的X,Y并继续递归寻找 */
        for (int i = 0; i < 4; i++) {
            int newX = x + dx[i];
            int newY = y + dy[i];
            /* 若越界 则返回 */
            if (newX < 0 || newX >= map[0].length || newY < 0 || newY >= map.length) {
                continue;
            }
            /* 若已经到过了就跳过 */
            if (map[newY][newX] != wall || visited[newY][newX] == true) {
                continue;
            }
            /* 将墙壁变成道路 */
            map[newY][newX] = road;
            /* 继续寻找 */
            Recursion(newY, newX, visited, depth + 1);
        }
    }

    public void printMap() {
        for (int[] row : map) {
            for (int column : row) {
                System.out.printf(column + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    @Override
    public void paint(Graphics g) {
        // TODO Auto-generated method stub
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == wall) {
                    g.setColor(Color.BLACK);
                } else if (map[i][j] == begin) {
                    g.setColor(Color.GREEN);
                } else if (map[i][j] == dest) {
                    g.setColor(Color.RED);
                } else {
                    g.setColor(Color.WHITE);
                }
                g.fillRect(j * blockSize, i * blockSize, blockSize, blockSize);
            }
        }
    }
}
