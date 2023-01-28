package NonLinear;
import java.util.Random;

import Basic.Structure.BinaryTree;
import Basic.Structure.Node.Treenode;

public class 相同的树 {
    public static void main(String[] args) {
        BinaryTree<Integer> Tree_a=new BinaryTree<Integer>(50);
        BinaryTree<Integer> Tree_b=new BinaryTree<Integer>(50);
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
        System.out.println(isSameTree(Tree_a.getRoot(), Tree_b.getRoot()));
    }
    public static boolean isSameTree(Treenode<Integer> p, Treenode<Integer> q) {
        if (p == null && q == null) {
            return true;
        } else if (p == null || q == null) {
            return false;
        } else if (p.data != q.data) {
            return false;
        } else {
            return isSameTree(p.Left, q.Left) && isSameTree(p.Right, q.Right);
        }
    }
}
