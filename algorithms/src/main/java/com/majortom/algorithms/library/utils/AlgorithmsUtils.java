package com.majortom.algorithms.library.utils;

import java.util.*;
import com.majortom.algorithms.library.basic.node.ListNode;
import com.majortom.algorithms.library.basic.tree.TreeNode;
import com.majortom.algorithms.library.basic.tree.BinaryTreeNode;

/**
 * 算法实验室工具类。
 */
public abstract class AlgorithmsUtils {
    private static final Random random = new Random();
    private static final java.lang.String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final java.lang.String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final java.lang.String NUMBERS = "0123456789";
    private static final java.lang.String CHARACTERS = UPPER_CASE + LOWER_CASE + NUMBERS;

    /**
     * 内部工具类：提供一个具体的二叉节点实现，用于静态树的构建
     */
    private static class SimpleBinaryNode<T> extends BinaryTreeNode<T> {
        private static final java.util.concurrent.atomic.AtomicLong IDS = new java.util.concurrent.atomic.AtomicLong(1L);
        private final long id = IDS.getAndIncrement();

        public SimpleBinaryNode(T data) {
            super(data);
        }
    }

    // --- 数组工具 ---

    public static Integer[] randomArray(int cap, int max) {
        Integer[] arr = new Integer[cap];
        for (int i = 0; i < cap; i++) {
            arr[i] = random.nextInt(max);
        }
        return arr;
    }

    public static Integer[] nearlySortedArray(int cap, int swapTimes) {
        Integer[] arr = new Integer[cap];
        for (int i = 0; i < cap; i++)
            arr[i] = i;
        for (int i = 0; i < swapTimes; i++) {
            swap(arr, random.nextInt(cap), random.nextInt(cap));
        }
        return arr;
    }

    public static Integer[] reversedArray(int cap) {
        Integer[] arr = new Integer[cap];
        for (int i = 0; i < cap; i++) {
            arr[i] = cap - i;
        }
        return arr;
    }

    public static int[] toPrimitive(Integer[] arr) {
        if (arr == null)
            return null;
        int[] res = new int[arr.length];
        for (int i = 0; i < arr.length; i++)
            res[i] = arr[i];
        return res;
    }

    public static Integer[] copy(Integer[] arr) {
        return arr == null ? null : arr.clone();
    }

    public static <T> void swap(T[] arr, int i, int j) {
        if (i == j)
            return;
        T temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    public static <T extends Comparable<T>> boolean isSorted(T[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i].compareTo(arr[i + 1]) > 0)
                return false;
        }
        return true;
    }

    public static java.lang.String[] randomStringArray(int cap, int length) {
        java.lang.String[] array = new java.lang.String[cap];
        for (int i = 0; i < cap; i++) {
            array[i] = randomString(length, CHARACTERS);
        }
        return array;
    }

    private static java.lang.String randomString(int length, java.lang.String charSet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(charSet.charAt(random.nextInt(charSet.length())));
        }
        return sb.toString();
    }

    public static Integer randomNum(int max, int step) {
        return random.nextInt(max) + step;
    }

    // --- 链表工具 ---

    public static <T> ListNode<T> buildLinkedList(T[] nums) {
        return buildLinkedListInternal(nums, 0);
    }

    private static <T> ListNode<T> buildLinkedListInternal(T[] nums, int index) {
        if (index == nums.length)
            return null;
        ListNode<T> node = new ListNode<>(nums[index]);
        node.setNext(buildLinkedListInternal(nums, index + 1));
        return node;
    }

    public static <T> void printList(ListNode<T> head) {
        ListNode<T> curr = head;
        while (curr != null) {
            System.out.print(curr.getValue() + " -> ");
            curr = curr.getNext();
        }
        System.out.println("null");
    }

    // --- 树工具 (核心适配) ---

    public static <T extends Comparable<T>> BinaryTreeNode<T> buildBST(T[] data) {
        if (data == null || data.length == 0)
            return null;
        Arrays.sort(data);
        return buildBSTInternal(data, 0, data.length - 1);
    }

    private static <T extends Comparable<T>> BinaryTreeNode<T> buildBSTInternal(T[] data, int start, int end) {
        if (start > end)
            return null;
        int mid = start + (end - start) / 2;

        BinaryTreeNode<T> node = new SimpleBinaryNode<>(data[mid]);
        node.setLeft(buildBSTInternal(data, start, mid - 1));
        node.setRight(buildBSTInternal(data, mid + 1, end));

        return node;
    }

    /**
     * 层序遍历构建二叉树 [1, 2, 3, null, 5]
     */
    public static <T> TreeNode<T> buildTreeByLevel(T[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null)
            return null;

        BinaryTreeNode<T> root = new SimpleBinaryNode<>(arr[0]);
        Queue<BinaryTreeNode<T>> queue = new ArrayDeque<>();
        queue.add(root);

        int i = 1;
        while (!queue.isEmpty() && i < arr.length) {
            BinaryTreeNode<T> curr = queue.poll();

            if (i < arr.length && arr[i] != null) {
                curr.setLeft(new SimpleBinaryNode<>(arr[i]));
                queue.add(curr.getLeft());
            }
            i++;

            if (i < arr.length && arr[i] != null) {
                curr.setRight(new SimpleBinaryNode<>(arr[i]));
                queue.add(curr.getRight());
            }
            i++;

        }
        return root;
    }

    // --- 图与迷宫工具 ---

    public static Integer[][] buildGraph(int n, double density) {
        Integer[][] matrix = new Integer[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j)
                    matrix[i][j] = 0;
                else if (random.nextDouble() < density)
                    matrix[i][j] = random.nextInt(10) + 1;
                else
                    matrix[i][j] = null;
            }
        }
        return matrix;
    }

    public static int[][] buildMaze(int rows, int cols, double wallProbability) {
        int[][] maze = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                maze[i][j] = (random.nextDouble() < wallProbability) ? 1 : 0;
            }
        }
        maze[0][0] = 0;
        maze[rows - 1][cols - 1] = 0;
        return maze;
    }

    // --- 辅助类 ---

    public static class Stopwatch {
        private final long start = System.currentTimeMillis();

        public double elapsedTime() {
            return (System.currentTimeMillis() - start) / 1000.0;
        }
    }

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

    public static void display(Object[] arr) {
        System.out.println(Arrays.toString(arr));
    }
}
