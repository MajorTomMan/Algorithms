package com.majortom.algorithms.app.leetcode.linear;

import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Queue;

public class 迷宫回溯问题 {
    public static void main(String[] args) {
        Integer[][] map = getMap();
        // 输出地图
        setEnd(map, 6, 5, 5);
        setStart(map, 1, 1, 3);
        System.out.println("地图的初始情况:");
        showMap(map);
        System.out.println("--------------------start-------------------");
        System.out.println(findWay(map, 1, 1,3, 5));
    }

    /**
     * 创建一个二维数组,用于模拟8*7迷宫
     * 使用1表示不可通过的实心方块，0表示可通过砖块
     * （6,5）为默认终点，（1,1）为默认起点
     * 
     * @return
     */
    private static Integer[][] getMap() {
        Integer[][] map = new Integer[8][7];
        // 上下全置为1
        for (int i = 0; i < 7; i++) {
            map[0][i] = 1;
            map[7][i] = 1;
        }
        // 左右全置为1
        for (int i = 0; i < 8; i++) {
            map[i][0] = 1;
            map[i][6] = 1;
        }
        // 设置挡板
        map[3][1] = 1;
        map[3][2] = 1;
        return map;
    }

    /**
     * 设置目标位置和指定数
     * 
     * 
     * @param map    地图二维数组
     * @param x      起始点横坐标
     * @param y      起始点纵坐标
     * @param target 目标数
     * @return
     */
    private static void setEnd(Integer[][] map, int x, int y, int end) {
        map[x][y] = end;
    }

    private static void setStart(Integer[][] map, int x, int y, int start) {
        map[x][y] = start;
    }

    /**
     * 展示地图
     * 
     * @param map
     */
    private static void showMap(Integer[][] map) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 7; j++) {
                System.out.print(map[i][j] + " ");
            }
            System.out.println();
        }
    }

    /**
     * 给定起始点，根据地图找路
     * 使用2表示可以走通的路，使用3表示走过但是不通的路
     * 
     * @param map 地图二维数组
     * @param x   起始点横坐标
     * @param y   起始点纵坐标
     * @return
     */
    public static boolean findWay(Integer[][] map, int x, int y, int start, int end) {
        // 如果走到了终点就终止
        if (map[y][x] == end) {
            return true;
        } else {
            if (map[y][x] == start){
                x=x+1;
            }
            // 只有为0的路才能通过
            if (map[y][x] == 0) {
                // 如果该点可以走通就打上标记
                map[y][x] = 2;
                showMap(map);
                System.out.println("----------every step------------");
                if (findWay(map, x, y + 1, start, end)) {
                    // 向下递归
                    return true;
                } else if (findWay(map, x + 1, y, start, end)) {
                    // 向右递归
                    return true;
                } else if (findWay(map, x, y - 1, start, end)) {
                    // 向上递归
                    return true;
                } else if (findWay(map, x - 1, y, start, end)) {
                    // 向左递归
                    return true;
                } else {
                    // 都走不通说明是死胡同
                    map[y][x] = 3;
                    return false;
                }
            } else {
                // 不为0说明要么是死路要么是障碍
                return false;
            }
        }
    }

    /**
     * 给定起始点，根据地图找路,利用BFS算法(广度优先)
     * 使用-1表示可以走通的路
     * 
     * @param map 地图二维数组
     * @param x   起始点横坐标
     * @param y   起始点纵坐标
     * @return
     */
    public static boolean BFS(Integer[][] map, int x, int y, int target) {
        Queue<Integer> queue = new LinkedList<>();
        queue.add(map[x][y]);
        map[x][y] = -1;
        while (!queue.isEmpty()) {
            int currentPoint = queue.poll();
            for (int i = y; i != map[x].length - 1 && x != map.length - 1; i++) {
                if (map[x][i] == target) {
                    System.out.println("Found!");
                    showMap(map);
                    System.out.println("----------every step------------");
                    return true;
                }
                queue.add(map[x][i]);
                if (map[x][i] != 1) {
                    map[x][i] = -1;
                } else {
                    continue;
                }
                map[x][i] = 0;
                showMap(map);
                System.out.println("----------every step------------");
            }
            if (x < map.length - 1) {
                x++;
            }
        }
        return false;
    }
}
