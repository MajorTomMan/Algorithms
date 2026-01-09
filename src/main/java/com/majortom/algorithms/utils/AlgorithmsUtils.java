package com.majortom.algorithms.utils;

import java.util.Random;

import com.majortom.algorithms.core.basic.node.ListNode;
import com.majortom.algorithms.core.tree.node.TreeNode;

import java.util.Arrays;

/**
 * 算法实验室工具类 v2.0
 * 涵盖：随机生成、类型转换、状态校验、性能监测
 */
public abstract class AlgorithmsUtils {
    private static final Random random = new Random();
    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String CHARACTERS = UPPER_CASE + LOWER_CASE + NUMBERS;

    /**
     * 生成随机 Integer 数组
     * 
     * @param cap 长度
     * @param max 最大值(不含)
     */
    public static Integer[] randomArray(int cap, int max) {
        Integer[] arr = new Integer[cap];
        for (int i = 0; i < cap; i++) {
            arr[i] = random.nextInt(max);
        }
        return arr;
    }

    /**
     * 生成近乎有序的数组 (适合测试插入排序等算法的敏感度)
     * 
     * @param swapTimes 随机交换的次数，次数越少越有序
     */
    public static Integer[] nearlySortedArray(int cap, int swapTimes) {
        Integer[] arr = new Integer[cap];
        for (int i = 0; i < cap; i++)
            arr[i] = i;
        for (int i = 0; i < swapTimes; i++) {
            swap(arr, random.nextInt(cap), random.nextInt(cap));
        }
        return arr;
    }

    /**
     * 生成完全倒序的数组
     */
    public static Integer[] reversedArray(int cap) {
        Integer[] arr = new Integer[cap];
        for (int i = 0; i < cap; i++) {
            arr[i] = cap - i;
        }
        return arr;
    }

    /**
     * 将包装类数组 Integer[] 转为基本类型 int[] (可视化渲染通常需要这个)
     */
    public static int[] toPrimitive(Integer[] arr) {
        if (arr == null)
            return null;
        int[] res = new int[arr.length];
        for (int i = 0; i < arr.length; i++)
            res[i] = arr[i];
        return res;
    }

    /**
     * 浅拷贝数组（生成快照）
     */
    public static Integer[] copy(Integer[] arr) {
        return arr == null ? null : arr.clone();
    }

    /**
     * 泛用交换方法
     */
    public static <T> void swap(T[] arr, int i, int j) {
        if (i == j)
            return;
        T temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    /**
     * 校验是否有序 (泛型版)
     */
    public static <T extends Comparable<T>> boolean isSorted(T[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i].compareTo(arr[i + 1]) > 0)
                return false;
        }
        return true;
    }

    public static String[] randomStringArray(int cap, int length) {
        String[] array = new String[cap];
        for (int i = 0; i < cap; i++) {
            array[i] = randomString(length, CHARACTERS);
        }
        return array;
    }

    private static String randomString(int length, String charSet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(charSet.charAt(random.nextInt(charSet.length())));
        }
        return sb.toString();
    }

    public static Integer randomNum(int max, int step) {

        return random.nextInt(max) + step;

    }

    /**
     * 极简秒表计时器
     */
    public static class Stopwatch {
        private final long start;

        public Stopwatch() {
            start = System.currentTimeMillis();
        }

        public double elapsedTime() {
            long now = System.currentTimeMillis();
            return (now - start) / 1000.0;
        }
    }

    /**
     * 打印二维图（邻接矩阵）并对齐
     */
    public static void printMatrix(Integer[][] matrix) {
        for (Integer[] row : matrix) {
            System.out.print("[");
            for (int j = 0; j < row.length; j++) {
                System.out.printf("%3d", row[j]);
                if (j < row.length - 1)
                    System.out.print(",");
            }
            System.out.println("]");
        }
    }

    /**
     * 打印普通数组
     */
    public static void display(Object[] arr) {
        System.out.println(Arrays.toString(arr));
    }

    /**
     * 根据数组构建单链表 (递归实现)
     */
    public static <T> ListNode<T> buildLinkedList(T[] nums) {
        return buildLinkedListInternal(nums, 0);
    }

    private static <T> ListNode<T> buildLinkedListInternal(T[] nums, int index) {
        if (index == nums.length)
            return null;
        ListNode<T> node = new ListNode<>(nums[index]);
        node.next = buildLinkedListInternal(nums, index + 1);
        return node;
    }

    /**
     * 打印链表结构： 1 -> 2 -> 3 -> null
     */
    public static <T> void printList(ListNode<T> head) {
        ListNode<T> curr = head;
        while (curr != null) {
            System.out.print(curr.data + " -> ");
            curr = curr.next;
        }
        System.out.println("null");
    }

    /**
     * 根据有序数组构建平衡二叉搜索树 (BST) - 单节点版
     */
    public static <T extends Comparable<T>> TreeNode<T> buildBST(T[] data) {
        if (data == null || data.length == 0)
            return null;
        // 确保数组有序，才能构建出 BST
        Arrays.sort(data);
        return buildBSTInternal(data, 0, data.length - 1);
    }

    private static <T extends Comparable<T>> TreeNode<T> buildBSTInternal(T[] data, int start, int end) {
        if (start > end)
            return null;

        int mid = start + (end - start) / 2;
        TreeNode<T> node = new TreeNode<>(data[mid]);

        node.left = buildBSTInternal(data, start, mid - 1);
        node.right = buildBSTInternal(data, mid + 1, end);
        return node;
    }

    /**
     * 层序遍历构建二叉树
     * 输入：[1, 2, 3, null, 5]
     * 构建出以 1 为根，左孩子 2，右孩子 3，2 的右孩子为 5 的树
     */
    public static <T> TreeNode<T> buildTreeByLevel(T[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null)
            return null;

        TreeNode<T> root = new TreeNode<>(arr[0]);
        java.util.Queue<TreeNode<T>> queue = new java.util.LinkedList<>();
        queue.add(root);

        int i = 1;
        while (!queue.isEmpty() && i < arr.length) {
            TreeNode<T> curr = queue.poll();

            // 处理左孩子
            if (i < arr.length && arr[i] != null) {
                curr.left = new TreeNode<>(arr[i]);
                queue.add(curr.left);
            }
            i++;

            // 处理右孩子
            if (i < arr.length && arr[i] != null) {
                curr.right = new TreeNode<>(arr[i]);
                queue.add(curr.right);
            }
            i++;
        }
        return root;
    }

    /**
     * 构建随机邻接矩阵 (带权图)
     * 
     * @param n       节点数
     * @param density 边密度 (0.0 - 1.0)
     */
    public static Integer[][] buildGraph(int n, double density) {
        Integer[][] matrix = new Integer[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0; // 自环为0
                } else if (random.nextDouble() < density) {
                    matrix[i][j] = random.nextInt(10) + 1; // 随机权重
                } else {
                    matrix[i][j] = null; // 无边连接
                }
            }
        }
        return matrix;
    }

    /**
     * 构建随机迷宫 (0为路, 1为墙)
     */
    public static int[][] buildMaze(int rows, int cols, double wallProbability) {
        int[][] maze = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                maze[i][j] = (random.nextDouble() < wallProbability) ? 1 : 0;
            }
        }
        maze[0][0] = 0; // 起点必通
        maze[rows - 1][cols - 1] = 0; // 终点必通
        return maze;
    }
}