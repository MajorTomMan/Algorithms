package Basic.Structure;

import java.util.Arrays;
import java.util.Collections;

/**
 * Maze
 */
public class Maze {
  private int[][] maze;

  /**
   * @param maze 基准地图
   * @param cols 行数
   * @param rows 列数
   */
  public Maze(int cols, int rows) {
    this.maze = new int[cols][rows];
    for (int i = 0; i < maze.length; i++) {
      for (int j = 0; j < maze[0].length; j++) {
        maze[i][j] = 1;
      }
    }
  }

  public void generateMaze(int x, int y) {
    // 定义四个方向：上、下、左、右
    int[] dx = { -1, 1, 0, 0 };
    int[] dy = { 0, 0, -1, 1 };
    // 随机打乱四个方向的顺序
    Collections.shuffle(Arrays.asList(dx, dy));

    for (int i = 0; i < 4; i++) {
      int newX = x + dx[i];
      int newY = y + dy[i];
      if (newX >= 0 && newX < maze.length && newY >= 0 && newY < maze[0].length && maze[newX][newY] == 0) {
        // 如果当前位置的四周有可以通行的位置，则把墙壁打通
        maze[x][y] = 0;
        print(maze);
        generateMaze(newX, newY);
      }
    }
  }

  private void print(int[][] map) {
    for (int i = 0; i < map.length; i++) {
      System.out.print("[");
      for (int j = 0; j < map[i].length; j++) {
        if (j == map[i].length - 1) {
          System.out.print(map[i][j]);
        } else {
          System.out.print(map[i][j] + ",");
        }
      }
      System.out.println("]");
    }
  }

  /**
   * @return 返回地图
   */
  public int[][] getMaze() {
    return maze;
  }


  
}