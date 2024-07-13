package utils;

import java.util.Random;

import basic.structure.node.ListNode;
import basic.structure.node.TreeNode;

public abstract class AlgorithmsUtils {
    private static Random random = new Random();

    /**
     * @description: 生成随机数组
     * @return {*}
     */
    public static Integer[] randomArray(Integer cap, Integer max) {
        Integer[] arr = new Integer[cap];
        for (Integer i = 0; i < arr.length; i++) {
            arr[i] = random.nextInt(max);
        }
        return arr;
    }

    /**
     * @param <T>
     * @description: 构建一个链表
     * @return {*}
     */
    public static <T> ListNode<T> buildLinkedList(T[] nums) {
        return buildLinkedList(null, nums, 0);
    }

    public static <T> void printList(ListNode<T> node) {
        System.out.println(node);
    }

    private static <T> ListNode<T> buildLinkedList(ListNode<T> node, T[] nums, Integer index) {
        if (index == nums.length) {
            return null;
        }
        if (node == null) {
            node = new ListNode<T>(nums[index]);
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
     * @return Integer[][] 一张随机图
     */
    public static Integer[][] buildGraph(Integer n, Integer m) {
        Integer[][] map = new Integer[n][m];
        Random random = new Random();
        for (Integer i = 0; i < map.length; i++) {
            for (Integer j = 0; j < map[i].length; j++) {
                map[i][j] = random.nextInt(10) + 1;
            }
        }
        return map;
    }

    public static void printGraph(Integer[][] map) {
        for (Integer i = 0; i < map.length; i++) {
            System.out.print("[");
            for (Integer j = 0; j < map[i].length; j++) {
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
        for (Integer i = 0; i < map.length; i++) {
            System.out.print("[");
            for (Integer j = 0; j < map[i].length; j++) {
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
     * @param {Integer[]} 数组
     * @param {Integer}   待交换的数
     * @param {Integer}   待交换的数
     * @return {*}
     */
    public static void swap(Integer[] arr, Integer i, Integer j) {
        if (i == j)
            return;
        arr[i] = arr[i] ^ arr[j];
        arr[j] = arr[i] ^ arr[j];
        arr[i] = arr[i] ^ arr[j];
    }

    /**
     * @description: 排序时的比较方法
     * @param {Integer} 待交换的数
     * @param {Integer} 待交换的数
     * @return {*}
     */
    public static boolean less(Integer i, Integer j) {
        if (i <= j) {
            return false;
        }
        return true;
    }

    public static void display(Integer[] arr) {
        for (Integer i : arr) {
            System.out.print(i + " ");
        }
        System.out.println();
    }

    public static Integer randomNum(int max, int step) {
        return random.nextInt(max) + step;
    }

    public static void printGraph(Integer[][] graph, boolean[] visited) {
        boolean isPrint = false;
        for (int i = 0; i < graph.length; i++) {
            for (int j = 0; j < graph[0].length; j++) {
                System.out.printf(graph[i][j] + " ");
            }
            if (!isPrint) {
                System.out.print("\t");
                for (int j = 0; j < graph[0].length; j++) {
                    System.out.printf(visited[j] + " ");
                }
                isPrint = true;
            }
            System.out.println();
        }
    }

    public static void printGraph(Integer[][] graph, boolean[][] visited) {
        for (int i = 0; i < graph.length; i++) {
            for (int j = 0; j < graph[0].length; j++) {
                System.out.printf(graph[i][j] + " ");
            }
            System.out.print("\t");
            for (int j = 0; j < graph[0].length; j++) {
                System.out.printf(visited[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("--------------------------------------------");
    }

    public static <Key extends Comparable<Key>, Value> TreeNode<Key, Value> buildTree(Key[] keys,
            Value[] values) {
        if (keys.length != values.length) {
            throw new IllegalArgumentException("Keys and values arrays must have the same length");
        }
        return buildTree(keys, values, 0, keys.length - 1);
    }

    private static <Key extends Comparable<Key>, Value> TreeNode<Key, Value> buildTree(Key[] keys, Value[] values,
            int start, int end) {
        if (start > end) {
            return null;
        }
        int mid = (start + end) / 2;
        TreeNode<Key, Value> node = new TreeNode<>(keys[mid], values[mid]);
        node.left = buildTree(keys, values, start, mid - 1);
        node.right = buildTree(keys, values, mid + 1, end);
        return node;
    }
}
