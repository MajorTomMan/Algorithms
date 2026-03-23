package com.majortom.algorithms.app.leetcode.others;

import java.util.HashMap;
import java.util.Map;

import com.majortom.algorithms.core.basic.HuffmanTree;
import edu.princeton.cs.algs4.Huffman;

public class 霍夫曼编码测试 {
    public static void main(String[] args) {
        String data ="ABRACADABRA!"; //字符
        char[] input=data.toCharArray();
        Huffman.compress();
        Huffman.expand();
        System.out.println();
        another_Tree();
    }
    public static void another_Tree(){
        HuffmanTree<String> tree=new HuffmanTree<>();
        Map<String,Integer> map=new HashMap<>(){
            {
                put("A", 3);
                put("C", 4);
                put("P", 2);
                put("Q", 7);
                put("S", 1);
                put("Z", 5);
            }
        };
        tree.buildBST(map);
        tree.printTree();
    }
}
