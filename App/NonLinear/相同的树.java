package NonLinear;
import java.util.Random;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

public class 相同的树 {
    public static void main(String[] args) {
        BinaryTree<Integer> Tree_a=new BinaryTree<Integer>();
        BinaryTree<Integer> Tree_b=new BinaryTree<Integer>();
        int i=0;
        Random random=new Random();
        System.out.println("-----------------------raw data--------------------");
        while(i!=12){
            int n=random.nextInt(100);
            int m=random.nextInt(100);
            Tree_a.put(n);
            Tree_b.put(n);
            System.out.print(n+" "+m+" \n");
            i++;
        }
        System.out.println("-------------------------");
    }
    public static boolean isSameTree(TreeNode<Integer> p, TreeNode<Integer> q) {
        if (p == null && q == null) {
            return true;
        } else if (p == null || q == null) {
            return false;
        } else if (p.data != q.data) {
            return false;
        } else {
            return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
        }
    }
}
