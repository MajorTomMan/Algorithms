package Basic.Structure.Maze;

import java.util.List;

public class DFS {
    private static boolean[][] visited;
    private static boolean isInitial = false;

    public static boolean searchPath(int[][] maze, int x, int y, List<Integer> path) {
        if (isInitial == false) {
            visited = new boolean[maze.length][maze[0].length];
            isInitial = true;
        }
        if (maze[y][x] == 9) {
            path.add(x);
            path.add(y);
            visited[y][x] = true;
            return true;
        }
        if (maze[y][x] == 0|| maze[y][x] == 3) {
            maze[y][x] = 2;
            int dx = -1;
            int dy = 0;
            if (searchPath(maze, x + dx, y + dy, path)) {
                path.add(x);
                path.add(y);
                visited[y][x] = true;
                return true;
            }
            dx = 1;
            dy = 0;
            if (searchPath(maze, x + dx, y + dy, path)) {
                path.add(x);
                path.add(y);
                visited[y][x] = true;
                return true;
            }
            dx = 0;
            dy = 1;
            if (searchPath(maze, x + dx, y + dy, path)) {
                path.add(x);
                path.add(y);
                visited[y][x] = true;
                return true;
            }
            dx = 0;
            dy = -1;
            if (searchPath(maze, x + dx, y + dy, path)) {
                path.add(x);
                path.add(y);
                visited[y][x] = true;
                return true;
            }
        }
        return false;
    }
}
