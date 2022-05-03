package NonLinear;

import java.util.Random;

import Basic.Structure.BRTree;
import Basic.Structure.Tree;
import Basic.Structure.Node.Treenode;

public class 轴对称树 {
    public static void main(String[] args) {
        int i=0;
        BRTree<Integer> Tree=new BRTree<Integer>(1);
        Random random=new Random();
        Tree.Insert(2);
        Tree.Insert(2);
        Tree.Insert(3);
        Tree.Insert(4);
        Tree.Insert(3);
        Tree.Insert(4);
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
