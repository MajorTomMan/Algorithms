package nonlinear;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import basic.structure.BinaryTree;
import basic.structure.node.TreeNode;

public class 二叉树的中序遍历 {
    public static void main(String[] args) {
        BinaryTree<Integer> Tree=new BinaryTree<Integer>();
        int i=0;
        Random random=new Random();
        System.out.println("-----------------------raw data--------------------");
        while(i!=12){
            int ran=random.nextInt(100);
            Tree.put(ran);
            System.out.print(ran+" ");
            i++;
        }
        System.out.println();

    }
    public static List<Integer> inorderTraversal(TreeNode<Integer> root) {
        List<Integer> list=new ArrayList<>();
        inorderTraversal(root,list);
        return list;
    }
    private static TreeNode<Integer> inorderTraversal(TreeNode<Integer> node,List<Integer> list) {
        if(node==null){
            return node;
        }
        inorderTraversal(node.left, list);
        list.add(node.data);
        inorderTraversal(node.right, list);
        return node;
    }
}
