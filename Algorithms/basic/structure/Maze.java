package basic.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Queue;
import java.util.LinkedList;

/**
 * Maze
 */
public class Maze {
    private int[][] map;
    private int start = 2;
    private int end = 3;
    private int path = 0;
    private int wall = 1;
    private int walk = 5;
    private int width; // 宽度
    private int height; // 长度
    private int startX;
    private int startY;
    private int endX;
    private int endY;
    /* 代表着上下左右 */
    private int[][] direction = { { -1, 0 }, { 0, -1 }, { 1, 0 }, { 0, 1 } };
    private boolean[][] visited;
    private List<Integer> list = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
    /* 基础的并查集结构 */
    private UnionFind unionFind;

    public Maze(int width, int height) {
        map = new int[width][height];
        visited = new boolean[width][height];
        this.width = width;
        this.height = height;
        init();
        randomStart();
        randomEnd();
        unionFind = new UnionFind(width * height);
    }

    public void generatorMap() {
        startGenerator();
    }

    private void startGenerator() {
        // startDFSGenerator(startY, startX, visited, 2);
        startBFSGenerator();
        generatorUnionFind();
    }
    /* 利用并查集来合并查询每个单元格的集合以便于之后查询联通 */
    private void generatorUnionFind() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                /* 如果不是墙 */
                if (map[i][j] != wall) {
                    /* 获取当前位置在并查集中的索引 */
                    int currentIndex = getIndex(i, j);
                    if (j < height - 1 && map[i][j + 1] != wall) {
                        /* 合并两个索引 */
                        int rightIndex = getIndex(i, j + 1);
                        unionFind.union(currentIndex, rightIndex);
                    }
                    if (j > 0 && map[i][j - 1] != wall) {
                        int leftIndex = getIndex(i, j - 1);
                        unionFind.union(currentIndex, leftIndex);
                    }
                    if (i > 0 && map[i - 1][j] != wall) {
                        int upIndex = getIndex(i - 1, j);
                        unionFind.union(currentIndex, upIndex);
                    }
                    if (i < width - 1 && map[i + 1][j] != wall) {
                        int downIndex = getIndex(i + 1, j);
                        unionFind.union(currentIndex, downIndex);
                    }
                }
            }
        }
    }
    /* 将二维数组中的xy映射成一维数组中的索引 */
    public int getIndex(int x, int y) {
        return x * width + y;
    }

    private void startDFSGenerator(int currentY, int currentX, boolean[][] visited, int depth) {
        /* 检查边界和已访问 */
        if (map[currentY][currentX] == end) {
            visited[currentY][currentX] = true;
            return;
        }
        if (isArraysOverFlow(currentX, currentY)) {
            return;
        }
        if (isVisited(currentX, currentY, visited)) {
            return;
        }
        /* 不是终点或者起点就变成路 */
        if (map[currentY][currentX] != start) {
            map[currentY][currentX] = path;
        }
        visited[currentY][currentX] = true;
        /* 随机选择方向 */
        /* 分别对应上下左右 */
        List<Integer> list = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(list);
        for (Integer integer : list) {
            /* 获取新位置 */
            int newY = currentY + 2 * direction[integer][0];
            int newX = currentX + 2 * direction[integer][1];
            if (!isArraysOverFlow(newX, newY) && map[newY][newX] == wall) {
                map[currentY + direction[integer][0]][currentX + direction[integer][1]] = path;
                startDFSGenerator(newY, newX, visited, depth + 1);
            }
        }
        if (depth == 2) {
            map[currentY][currentX] = start;
            map[endY][endX] = end;
        }
    }

    private void startBFSGenerator() {
        visited = new boolean[width][height];
        Random random = new Random();
        int newX = random.nextInt(width), newY = random.nextInt(height);
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { startY, startX });
        visited[startY][startX] = true;
        while (!queue.isEmpty()) {
            int[] currentCell = queue.poll();
            newY = currentCell[0];
            newX = currentCell[1];
            if (newY > 0 && !visited[newY - 1][newX]) {
                /* 将未访问的节点以及方向加入队列 */
                queue.offer(new int[] { newY - 1, newX });
                visited[newY - 1][newX] = true;
                map[newY - 1][newX] = path;
            }
            if (newY < height - 1 && !visited[newY + 1][newX]) {
                queue.offer(new int[] { newY + 1, newX });
                visited[newY + 1][newX] = true;
                map[newY + 1][newX] = path;
            }
            if (newX > 0 && !visited[newY][newX - 1]) {
                queue.offer(new int[] { newY, newX - 1 });
                visited[newY][newX - 1] = true;
                map[newY][newX - 1] = path;
            }
            if (newX < width - 1 && !visited[newY][newX + 1]) {
                queue.offer(new int[] { newY, newX + 1 });
                visited[newY][newX + 1] = true;
                map[newY][newX + 1] = path;
            }
            if (newY == endY && newX == endX) {
                visited[newY][newX] = true; // 将终点的visited值设置为false
                break;
            }
        }
        map[startY][startX] = start;
        map[endY][endX] = end;
        addWalls();
    }

    private void addWalls() {
        Random random = new Random();
        for (int i = 1; i < width - 1; i += 2) {
            for (int j = 1; j < height - 1; j += 2) {
                if (map[i][j] == path) {
                    int direct = random.nextInt(direction.length);
                    switch (direct) {
                        case 0:
                            // 上方
                            if (map[i - 1][j] != end && map[i - 1][j] != start) {
                                map[i - 1][j] = wall;
                            }
                            break;
                        case 1:
                            // 下方
                            if (map[i + 1][j] != end && map[i - 1][j] != start) {
                                map[i + 1][j] = wall;
                            }
                            break;
                        case 2:
                            // 左侧
                            if (map[i][j - 1] != end && map[i - 1][j] != start) {
                                map[i][j - 1] = wall;
                            }
                            break;
                        case 3:
                            // 右侧
                            if (map[i][j + 1] != end && map[i][j + 1] != start) {
                                map[i][j + 1] = wall;
                            }
                            break;
                    }
                }
            }
        }
    }

    private void randomStart() {
        Random random = new Random();
        this.startX = random.nextInt(width - 1);
        this.startY = random.nextInt(height - 1);
        map[startY][startX] = start;
    }

    private void randomEnd() {
        Random random = new Random();
        this.endX = random.nextInt(width - 1);
        this.endY = random.nextInt(height - 1);
        map[endY][endX] = end;
    }

    private void init() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                map[i][j] = 1;
            }
        }
    }

    public boolean isConnected(boolean isUseUnionFind) {
        if (isUseUnionFind) {
            /* 生成起点和终点在一维数组中的映射 */
            int startIndex = getIndex(startY, startX);
            int endIndex = getIndex(endY, endX);
            return isConnected(startIndex, endIndex);
        } else {
            if (DFS(start, end, startY, startX, new boolean[width][height])) {
                return true;
            }
            return false;
        }
    }

    private boolean isConnected(int src, int dest) {
        return unionFind.find(src) == unionFind.find(dest);
    }

    /* DFS检测迷宫是否拥有一条从起点到终点的路 */
    private boolean DFS(int src, int dest, int currentY, int currentX, boolean[][] connect_visited) {
        if (map[currentY][currentX] == wall) {
            return false;
        }
        if (isArraysOverFlow(currentX, currentY)) {
            return false;
        }
        if (isVisited(currentX, currentY, connect_visited)) {
            return false;
        }
        if (currentY == endY && currentX == endX) {
            return true;
        }
        connect_visited[currentY][currentX] = true;
        for (int i = 0; i < list.size(); i++) {
            int newY = currentY + direction[list.get(i)][0];
            int newX = currentX + direction[list.get(i)][1];
            if (!isArraysOverFlow(newX, newY) && !isVisited(newX, newY, connect_visited)) {
                if (DFS(src, dest, newY, newX, connect_visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void print(int[][] map) {
        for (int[] is : map) {
            for (int i : is) {
                System.out.print(i + " ");
            }
            System.out.println();
        }
    }

    public void print() {
        for (int[] is : map) {
            for (int i : is) {
                System.out.print(i + " ");
            }
            System.out.println();
        }
    }

    public void printVisited() {
        for (boolean[] is : visited) {
            for (boolean i : is) {
                System.out.print(i + " ");
            }
            System.out.println();
        }
    }

    private boolean isArraysOverFlow(int currentX, int currentY) {
        if (currentX < 0 || currentX >= width || currentY < 0 || currentY >= height) {
            return true;
        }
        return false;
    }

    private boolean isVisited(int currentX, int currentY, boolean[][] _visited) {
        if (!isArraysOverFlow(currentX, currentY) && _visited[currentY][currentX]) {
            return true;
        }
        return false;
    }
}