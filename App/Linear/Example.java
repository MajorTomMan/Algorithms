
import java.util.Random;


import Basic.Structure.Node.Node;

public abstract class Example{
    public static Node<Integer> buildLinkedList(int nums[]) {
        return buildLinkedList(null, nums, 0);
    }

    public static void printLinkedList(Node<Integer> node) {
        System.out.println(node);
    }

    private static Node<Integer> buildLinkedList(Node<Integer> node, int nums[], int index) {
        if (index == nums.length) {
            return null;
        }
        if (node == null) {
            node = new Node<Integer>(nums[index], null);
        }
        node.next = buildLinkedList(node.next, nums, ++index);
        return node;
    }

    /**
     * 利用二维数组构建一张邻接矩阵(图)
     * 
     * @param n 宽
     * 
     * @param m 高
     * 
     * @return int[][] 一张随机图
     */
    public static int[][] buildGraph(int n, int m) {
        int[][] map = new int[n][m];
        Random random = new Random();
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                map[i][j] = random.nextInt(10) + 1;
            }
        }
        return map;
    }

    public static void printGraph(int[][] map) {
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

    public static void printGraph(boolean[][] map) {
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
}
