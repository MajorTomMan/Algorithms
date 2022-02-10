package NonLinear;

import Character.DataCompression.Huffman;

public class 霍夫曼编码测试 {
    public static void main(String[] args) {
        String data ="ABRACADABRA!"; //字符
        char[] input=data.toCharArray();
        Huffman huffman = new Huffman();
        huffman.createTree(input);
        huffman.printTree();
    }
}
