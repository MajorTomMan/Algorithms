package com.majortom.algorithms.utils;

import java.util.*;
import com.majortom.algorithms.core.basic.node.ListNode;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.node.Vertex;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.tree.node.BinaryTreeNode;

/**
 * 算法实验室工具类 v2.1
 * 适配：重构后的 TreeNode 体系及可视化元数据维护
 */
public abstract class AlgorithmsUtils {
    private static final Random random = new Random();
    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String CHARACTERS = UPPER_CASE + LOWER_CASE + NUMBERS;

    /**
     * 内部工具类：提供一个具体的二叉节点实现，用于静态树的构建
     */
    private static class SimpleBinaryNode<T> extends BinaryTreeNode<T> {
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

    // --- 链表工具 ---

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

    public static <T> void printList(ListNode<T> head) {
        ListNode<T> curr = head;
        while (curr != null) {
            System.out.print(curr.data + " -> ");
            curr = curr.next;
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
        node.left = buildBSTInternal(data, start, mid - 1);
        node.right = buildBSTInternal(data, mid + 1, end);

        // 关键：构建后刷新高度和规模，否则可视化画布无法分层
        refreshNodeMetrics(node);
        return node;
    }

    /**
     * 层序遍历构建二叉树 [1, 2, 3, null, 5]
     */
    public static <T> TreeNode<T> buildTreeByLevel(T[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null)
            return null;

        BinaryTreeNode<T> root = new SimpleBinaryNode<>(arr[0]);
        Queue<BinaryTreeNode<T>> queue = new LinkedList<>();
        queue.add(root);

        int i = 1;
        while (!queue.isEmpty() && i < arr.length) {
            BinaryTreeNode<T> curr = queue.poll();

            if (i < arr.length && arr[i] != null) {
                curr.left = new SimpleBinaryNode<>(arr[i]);
                queue.add(curr.left);
            }
            i++;

            if (i < arr.length && arr[i] != null) {
                curr.right = new SimpleBinaryNode<>(arr[i]);
                queue.add(curr.right);
            }
            i++;

            // 这里通常需要全量刷新，层序构建较难单点维护高度，建议最后统一处理或在循环内局部刷新
            refreshNodeMetrics(curr);
        }
        // 对于层序构建，最好从根部递归刷新一次确保全局高度正确
        refreshGlobalMetrics(root);
        return root;
    }

    /**
     * 局部刷新节点的 height 和 subTreeCount
     */
    private static <T> void refreshNodeMetrics(BinaryTreeNode<T> node) {
        if (node == null)
            return;
        int hL = (node.left == null) ? 0 : node.left.height;
        int hR = (node.right == null) ? 0 : node.right.height;
        int sL = (node.left == null) ? 0 : node.left.subTreeCount;
        int sR = (node.right == null) ? 0 : node.right.subTreeCount;

        node.height = 1 + Math.max(hL, hR);
        node.subTreeCount = 1 + sL + sR;
    }

    /**
     * 递归全局刷新树的元数据
     */
    private static <T> int refreshGlobalMetrics(TreeNode<T> node) {
        if (node == null)
            return 0;
        int maxH = 0;
        int totalS = 1;
        for (TreeNode<T> child : node.getChildren()) {
            if (child != null) {
                maxH = Math.max(maxH, refreshGlobalMetrics(child));
                totalS += child.subTreeCount;
            }
        }
        node.height = 1 + maxH;
        node.subTreeCount = totalS;
        return node.height;
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

    /**
     * 适配重构后的 BaseGraph 体系
     * 职责：通过 BaseGraph 暴露的接口构建随机图，并确保 ID 与数据的一致性
     */
    @SuppressWarnings("unchecked")

    public static <V> void buildRandomGraph(BaseGraph<V> graph, int nodeCount, int edgeCount, boolean isAlpha) {
        // 1. 重置图状态与统计数据
        graph.resetStatistics();
        // 如果 BaseGraph 还没有 clear 逻辑，建议在 BaseGraph 中补充 graph.clear()

        // 存储生成的 ID 列表，用于后续随机连接边
        List<String> nodeIds = new ArrayList<>();

        // 2. 构建节点
        for (int i = 0; i < nodeCount; i++) {
            String id;
            V data;

            if (isAlpha && i < 26) {
                id = String.valueOf((char) ('A' + i));
            } else {
                id = String.valueOf(i);
            }

            // 假设业务数据 data 与 ID 保持一致，或者是某种泛型转化
            data = (V) id;

            nodeIds.add(id);
            // 适配 BaseGraph.addVertex(String id, V data)
            graph.addVertex(id, data);
        }

        // 3. 构建边
        if (nodeIds.size() < 2)
            return;

        int actualEdges = 0;
        int maxAttempts = edgeCount * 3; // 适当放宽尝试次数
        int attempts = 0;

        while (actualEdges < edgeCount && attempts < maxAttempts) {
            attempts++;
            int fIdx = random.nextInt(nodeCount);
            int tIdx = random.nextInt(nodeCount);

            if (fIdx == tIdx)
                continue;

            String fromId = nodeIds.get(fIdx);
            String toId = nodeIds.get(tIdx);

            try {
                int weight = random.nextInt(10) + 1;
                // 适配 BaseGraph.addEdge(String fromId, String toId, int weight)
                graph.addEdge(fromId, toId, weight);
                actualEdges++;
            } catch (Exception e) {
                // 捕获 GraphStream 可能抛出的重复边异常 (IdAlreadyInUseException 等)
            }
        }
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