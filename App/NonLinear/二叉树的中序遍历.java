package NonLinear;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Basic.Structure.BRTree;
import Basic.Structure.Node.Treenode;

public class 二叉树的中序遍历 {
    public static void main(String[] args) {
        BRTree<Integer> Tree=new BRTree<Integer>(50);
        int i=0;
        Random random=new Random();
        System.out.println("-----------------------raw data--------------------");
        while(i!=12){
            int ran=random.nextInt(100);
            Tree.Insert(ran);
            System.out.print(ran+" ");
            i++;
        }
        System.out.println();
        System.out.println(inorderTraversal(Tree.getRoot()));
    }
    public static List<Integer> inorderTraversal(Treenode<Integer> root) {
        List<Integer> list=new ArrayList<>();
        inorderTraversal(root,list);
        return list;
    }
    private static Treenode<Integer> inorderTraversal(Treenode<Integer> node,List<Integer> list) {
        if(node==null){
            return node;
        }
        inorderTraversal(node.Left, list);
        list.add(node.data);
        inorderTraversal(node.Right, list);
        return node;
    }
}
