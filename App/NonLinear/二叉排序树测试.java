package NonLinear;


import java.util.Random;

import Basic.Structure.BRTree;

public class 二叉排序树测试{
    public static void main(String[] args) {
        int i=0;
        BRTree<Integer> Tree=new BRTree<Integer>(50);
        Random random=new Random();
        System.out.println("-----------------------raw data--------------------");
        while(i!=12){
            int ran=random.nextInt(100);
            Tree.put(ran);
            System.out.print(ran+",");
            i++;
        }
        Tree.put(34);
        System.out.println();
        Tree.Show();
        System.out.println("\n--------------");
        Tree.Delete(34);
        Tree.Show();
        System.out.println("\n--------------");
        System.out.println(Tree.Max());
        System.out.println(Tree.Min());
        System.out.println("\n--------------");
        System.out.println(Tree.getDepth());
        System.out.println(Tree.Size());
    }
}
