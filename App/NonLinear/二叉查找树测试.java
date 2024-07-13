package nonlinear;

import java.util.Random;

import search.structure.tree.BST;

public class 二叉查找树测试 {
    public static void main(String[] args) {
        int i=0;
        int min=65;
        int max=90;
        BST<Character,Integer> bst=new BST<>();
        Random random=new Random();
        while(i!=10){
            int temp=random.nextInt(max-min+1)+min;
            bst.put((char)temp,temp);
            i++;
        }
        for (char value:bst.keys()) {
            System.out.print(value+" "+bst.get(value));
            System.out.println();
        }
        System.out.println(bst.floor('F'));
        System.out.println(bst.ceiling('B'));
    }
}
