package Linear;


import java.util.Random;

import basic.structure.node.ListNode;

public abstract class Common {
    private static Random random = new Random();
    /**
     * @description: 生成随机数组
     * @return {*}
     */
    protected static int[] generatorRandomArray(int cap, int max) {
        int[] arr = new int[cap];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = random.nextInt(max);
        }
        return arr;
    }

    /**
     * @description: 构建一个链表
     * @return {*}
     */
    protected static ListNode<Integer> buildLinkedList(int nums[]) {
        return buildLinkedList(null, nums, 0);
    }

    protected static void printLinkedList(ListNode<Integer> node) {
        System.out.println(node);
    }

    private static ListNode<Integer> buildLinkedList(ListNode<Integer> node, int nums[], int index) {
        if (index == nums.length) {
            return null;
        }
        if (node == null) {
            node = new ListNode<Integer>(nums[index], null);
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
    protected static int[][] buildGraph(int n, int m) {
        int[][] map = new int[n][m];
        Random random = new Random();
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                map[i][j] = random.nextInt(10) + 1;
            }
        }
        return map;
    }

    protected static void printGraph(int[][] map) {
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

    protected static void printGraph(boolean[][] map) {
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
     * @description: 排序时用的交换方法
     * @param {int[]} 数组
     * @param {int} 待交换的数
     * @param {int} 待交换的数
     * @return {*}
     */
    protected static void swap(int[] arr, int i, int j) {
        if (i == j)
            return;
        arr[i] = arr[i] ^ arr[j];
        arr[j] = arr[i] ^ arr[j];
        arr[i] = arr[i] ^ arr[j];
    }

    /**
     * @description: 排序时的比较方法
     * @param {int} 待交换的数
     * @param {int} 待交换的数
     * @return {*}
     */
    protected static boolean less(int i, int j) {
        if (i <= j) {
            return false;
        }
        return true;
    }
    protected static void show(int[] arr){
        for (int i : arr) {
            System.out.print(i+" ");
        }
        System.out.println();
    }
}
