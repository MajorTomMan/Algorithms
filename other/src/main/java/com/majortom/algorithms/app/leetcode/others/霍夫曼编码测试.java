package com.majortom.algorithms.app.leetcode.others;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/** Historical Huffman exercise. The canonical V2 tree family does not currently expose HuffmanTree. */
public class 霍夫曼编码测试 {
    public static void main(String[] args) {
        Map<String, Integer> frequencies = new HashMap<>();
        frequencies.put("A", 3);
        frequencies.put("C", 4);
        frequencies.put("P", 2);
        frequencies.put("Q", 7);
        frequencies.put("S", 1);
        frequencies.put("Z", 5);
        HuffmanNode root = build(frequencies);
        print(root, "");
    }

    private static HuffmanNode build(Map<String, Integer> frequencies) {
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>(Comparator.comparingInt(HuffmanNode::weight));
        frequencies.forEach((symbol, weight) -> queue.add(new HuffmanNode(symbol, weight, null, null)));
        while (queue.size() > 1) {
            HuffmanNode left = queue.remove();
            HuffmanNode right = queue.remove();
            queue.add(new HuffmanNode(null, left.weight + right.weight, left, right));
        }
        return queue.remove();
    }

    private static void print(HuffmanNode node, String code) {
        if (node == null) {
            return;
        }
        if (node.symbol != null) {
            System.out.println("data:" + node.symbol + " code:" + code);
            return;
        }
        print(node.left, code + "0");
        print(node.right, code + "1");
    }

    private record HuffmanNode(String symbol, int weight, HuffmanNode left, HuffmanNode right) {
    }
}
