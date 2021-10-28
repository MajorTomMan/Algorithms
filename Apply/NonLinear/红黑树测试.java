package NonLinear;

import java.util.Random;

import Search.Structure.RedBlackBST;

public class 红黑树测试 {
    public static void main(String[] args) {
        RedBlackBST<Character,Integer> rBst=new RedBlackBST<>();
        int i=0;
        int min=65;
        int max=90;
        Random random=new Random();
        while(i!=10){
            int temp=random.nextInt(max-min+1)+min;
            rBst.put((char)temp,temp);
            i++;
        }
        for (char data:rBst.keys()) {
            System.out.print(data+" "+rBst.get(data));
            System.out.println();
        }
        System.out.println(rBst.floor('F'));
        System.out.println(rBst.ceiling('B'));
    }
}
