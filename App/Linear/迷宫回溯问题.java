public class 迷宫回溯问题 {
    public static void main(String[] args) {
        int[][] map=getMap();
        findWay(map,1,1);
    }

    /**
     * 创建一个二维数组,用于模拟8*7迷宫
     * 使用1表示不可通过的实心方块，0表示可通过砖块
     * （6,5）为默认终点，（1,1）为默认起点
     * 
     * @return
     */
    private static int[][] getMap() {
        int[][] map = new int[8][7];
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

        // 输出地图
        System.out.println("地图的初始情况:");
        showMap(map);

        return map;
    }

    /**
     * 展示地图
     * 
     * @param map
     */
    private static void showMap(int[][] map) {
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
    public static boolean findWay(int[][] map, int x, int y) {
        // 如果走到了终点就终止
        if (map[6][5] == 2) {
            return true;
        } else {
            // 只有为0的路才能通过
            if (map[y][x] == 0) {
                // 如果该点可以走通就打上标记
                map[y][x] = 2;
                showMap(map);
                System.out.println("----------every step------------");
                if (findWay(map, x, y + 1)) {
                    // 向下递归
                    return true;
                } else if (findWay(map, x + 1, y)) {
                    // 向右递归
                    return true;
                } else if (findWay(map, x, y - 1)) {
                    // 向上递归
                    return true;
                } else if (findWay(map, x - 1, y)) {
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
}
