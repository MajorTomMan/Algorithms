
import java.util.LinkedList;
import java.util.Queue;

public class 机器人的运动范围 extends Common {
    // 记录位置是否被遍历过
    private static boolean[][] visited;

    public static void main(String[] args) {
        System.out.println(movingCount(3, 2, 1));
    }
    /** 
     * 
     * @param m 行
     * @param n 列
     * @param k 行列和不能大于的上限数
     * @return 机器人能到达的格子数
    */
    public static int movingCount(int m, int n, int k) {
        
        visited = new boolean[m][n];
        return dfs(m, n, 0, 0, k);
    }

    private static int dfs(int m, int n, int i, int j, int k) {
        if (i >= m || j >= n) { // i >= m || j >= n是边界条件的判断
            return 0;
        }
        else if (visited[i][j] == true) { // visited[i][j]判断这个格子是否被访问过
            return 0;
        }
        else if (sum(i, j) > k) { // k < sum(i, j)判断当前格子坐标是否满足条件
            return 0;
        }
        visited[i][j] = true; // 标注这个格子被访问过
        return 1 + dfs(m, n, i + 1, j, k) + dfs(m, n, i, j + 1, k); // 沿着当前格子的右边和下边继续访问
    }

    private static int bfs(int m, int n, int k) {
        visited = new boolean[m][n];
        int res = 0;// 记录满足条件的方格数
        Queue<int[]> queue = new LinkedList<int[]>(); 
        queue.add(new int[] { 0, 0 });// 队列中加入此时i，j坐标，以供出队后拿来判断右、下
        while (!queue.isEmpty()) {
            int[] x = queue.poll();
            int i = x[0], j = x[1];
            if (i >= m || j >= n){ // 边界条件判断
                continue;
            }
            else if(sum(i, j) > k){ // sum(i,j) > k即不满足条件
                continue;
            }
            else if(visited[i][j] == true){ // 不满足则跳出循环
                continue;
            }
            visited[i][j] = true; // 记录当前节点被访问过
            res++;
            // 将下面的方格加入队列
            queue.add(new int[] { i + 1, j });
            // 将右面的方格加入队列
            queue.add(new int[] { i, j + 1 });
        }
        return res;
    }

    // 计算两个坐标数字的和
    private static int sum(int i, int j) {
        int sum = 0;
        while (i != 0) {
            sum += i % 10;
            i /= 10;
        }
        while (j != 0) {
            sum += j % 10;
            j /= 10;
        }
        return sum;
    }
}
