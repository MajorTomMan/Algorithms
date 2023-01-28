package NonLinear;

import Basic.Structure.BinaryTree;

public class 二叉排序树测试 extends Example{
    public static void main(String[] args) {
        BinaryTree<Integer> Tree=new BinaryTree<>();
        Tree.setRoot(buildTreeByRandom(10));
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
        printTree(Tree.getRoot());
        System.out.println("---------------------------");
        Tree.put(30);
        printTree(Tree.getRoot());
        Tree.Show();
    }
}
