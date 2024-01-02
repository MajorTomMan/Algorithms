package basic.structure;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HuffmanTree
 */
public class HuffmanTree {
    private class Node {
        private int weight;
        private Node leftNode;
        private Node rightNode;

        public Node(int weight, Node leftNode, Node rightNode) {
            this.weight = weight;
            this.leftNode = leftNode;
            this.rightNode = rightNode;
        }

    }

    private Node root;

    public HuffmanTree() {

    }

    /*
     * 自底向上构建霍夫曼树
     * 利用每一次循环的最小权重构建一个二叉树
     */
    public void generateTree(List<Integer> nodes) {
        List<Node> collect = nodes.stream().map(n -> {
            return new Node(n, null, null);
        }).collect(Collectors.toList());
        Comparator<Node> minComparator = new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                // TODO Auto-generated method stub
                return o1.weight - o2.weight;
            }
        };
        /* 利用两个最小的节点构建树并建立一个对应结构来储存 */
        while (collect.size() != 1) {
            Node min_l = Collections.min(collect, minComparator);
            collect.remove(min_l);
            Node min_r = Collections.min(collect, minComparator);
            collect.remove(min_r);
            Node root = new Node(min_l.weight + min_r.weight, min_l, min_r);
            collect.add(root);
        }
        root = collect.get(0);
    }

    public void printTree() {
        if (root == null) {
            return;
        }
        printTree(1, root);
    }

    private void printTree(int level, Node node) {
        if (node == null) {
            return;
        }
        System.out.print("|");
        for (int i = 0; i < level; i++) {
            System.out.printf("-");
        }
        if (node.leftNode == null && node.rightNode == null) {
            System.out.println(":" + node.weight + " ->叶子节点");
        } else {
            System.out.println(":" + node.weight + " ->第" + level + "层");
        }
        printTree(level + 1, node.leftNode);
        printTree(level + 1, node.rightNode);
    }
}