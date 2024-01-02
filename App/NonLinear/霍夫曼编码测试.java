package nonlinear;

import java.util.List;

import basic.structure.HuffmanTree;


public class 霍夫曼编码测试 {
    public static void main(String[] args) {
        HuffmanTree tree = new HuffmanTree();
        List<Integer> nodes = List.of(3,2,3,4,5,6,878,7523,31,231,23,123,1,-1,2,421,23,123,12,412,4,124,12,2412,41,23,12,3);
        tree.generateTree(nodes);
        tree.printTree();
    }

}
