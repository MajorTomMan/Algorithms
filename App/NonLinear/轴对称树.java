package NonLinear;

import java.util.Random;

import Basic.Structure.BinaryTree;
import Basic.Structure.Node.Treenode;

public class 轴对称树 {
    public static void main(String[] args) {
        int i=0;
        BinaryTree<Integer> Tree=new BinaryTree<Integer>(1);
        Random random=new Random();
        Tree.put(2);
        Tree.put(2);
        Tree.put(3);
        Tree.put(4);
        Tree.put(3);
        Tree.put(4);
        System.out.println();
        System.out.println(isSymmetric(Tree.getRoot()));
    }
    public static boolean isSymmetric(Treenode<Integer> root) {
        return isSymmetric(root.Left, root.Right);
    }
    private static boolean isSymmetric(Treenode<Integer> left,Treenode<Integer> right) {
        if(left==null||right==null){
            return false;
        }
        if(left.data==right.data||left==null&&right==null){
            return true;
        }
        return isSymmetric(left.Left, right.Right)&&isSymmetric(left.Right, right.Left);
    }
}
