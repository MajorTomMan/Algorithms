/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:58:28
 * @FilePath: /alg/App/NonLinear/相同的树.java
 */
package NonLinear;
import java.util.Random;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

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
    public static boolean isSameTree(TreeNode<Integer> p, TreeNode<Integer> q) {
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
