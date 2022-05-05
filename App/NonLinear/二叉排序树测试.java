package NonLinear;

import Basic.Structure.BRTree;

public class 二叉排序树测试 extends Example{
    public static void main(String[] args) {
        BRTree<Integer> Tree=buildTreeByRandom(12);
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
        System.out.println();
        System.out.println();
        Tree.Print();
    }
}
