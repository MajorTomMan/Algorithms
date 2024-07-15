package utils;

import java.util.Random;

import basic.structure.node.ListNode;
import basic.structure.node.TreeNode;

public abstract class AlgorithmsUtils {
    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String CHARACTERS = UPPER_CASE + LOWER_CASE + NUMBERS;
    private static Random random = new Random();

    public static String[] randomStringArray(int cap, int length) {
        return randomStringArray(cap, length, false, false, false);
    }

    public static String[] randomStringArray(int cap, int length, boolean isOnlyUpper) {
        return randomStringArray(cap, length, isOnlyUpper, false, false);
    }

    public static String[] randomStringArray(int cap, int length, boolean isOnlyUpper, boolean isOnlyLower) {
        return randomStringArray(cap, length, isOnlyUpper, isOnlyLower, false);
    }

    // 生成一个包含指定数量随机字符串的数组，每个字符串的长度为 stringLength
    public static String[] randomStringArray(int cap, int length, boolean isOnlyUpper, boolean isOnlyLower,
            boolean isOnlyNumber) {
        String[] array = new String[cap];
        String characters = CHARACTERS;

        if (isOnlyUpper) {
            characters = UPPER_CASE;
        } else if (isOnlyLower) {
            characters = LOWER_CASE;
        } else if (isOnlyNumber) {
            characters = NUMBERS;
        }

        for (int i = 0; i < cap; i++) {
            array[i] = randomString(length, characters);
        }
        return array;
    }

    // 生成一个指定长度的随机字符串，使用指定的字符集
    public static String randomString(int length, String characters) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < length; i++) {
            s.append(characters.charAt(random.nextInt(characters.length())));
        }
        return s.toString();
    }

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

    public static Integer[] sortedArray(Integer cap, Integer max) {
        if (cap < max) {
            throw new IllegalArgumentException("容量必须大于最大数值");
        }
        Integer[] arr = new Integer[cap];
        for (int i = 0; i <= max; i++) {
            arr[i] = i;
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

    @SuppressWarnings("unchecked")
    public static <Node, T, Key, Value> void display(Node node) {
        if (node instanceof ListNode) {
            doDisplay((ListNode<T>) node);
        } else if (node instanceof TreeNode) {
            doDisplay(0, (TreeNode<Key, Value>) node);
        }
    }

    private static <T, Key, Value> void doDisplay(int depth, TreeNode<Key, Value> node) {
        if (node == null) {
            return;
        }

        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }

        System.out.println("|- " + node.key + ": " + node.value);

        if (node.children != null) {
            for (TreeNode<Key, Value> child : node.children) {
                doDisplay(depth + 1, child);
            }
        }

        // Display left and right children if it's a binary tree node
        if (node.left != null) {
            doDisplay(depth + 1, node.left);
        }
        if (node.right != null) {
            doDisplay(depth + 1, node.right);
        }

    }

    private static <T> void doDisplay(ListNode<T> node) {
        if (node == null) {
            return;
        }
        System.out.println(node);
        doDisplay(node.next);
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
