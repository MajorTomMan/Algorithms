package nonlinear;

import java.util.HashMap;
import java.util.Map;

import basic.structure.HuffmanTree;
import character.datacompression.Huffman;

public class 霍夫曼编码测试 {
    public static void main(String[] args) {
        String data ="ABRACADABRA!"; //字符
        char[] input=data.toCharArray();
        Huffman huffman = new Huffman();
        huffman.createTree(input);
        huffman.printTree();
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
        tree.buildTree(map);
        tree.printTree();
    }
}
