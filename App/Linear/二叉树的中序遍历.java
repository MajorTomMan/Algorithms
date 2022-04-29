import java.util.List;
import java.util.Random;

import Basic.Structure.BRTree;
import Basic.Structure.Node.BTnode;

public class 二叉树的中序遍历 {
    public static void main(String[] args) {
        BRTree<Integer> Tree=new BRTree<Integer>(50);
        int i=0;
        Random random=new Random();
        System.out.println("-----------------------raw data--------------------");
        while(i!=12){
            int ran=random.nextInt(100);
            Tree.Insert(ran);
            System.out.print(ran+",");
            i++;
        }
        System.out.println(inorderTraversal(Tree.getRoot()));
    }
    public static List<Integer> inorderTraversal(BTnode<Integer> root) {
        return null;
    }
}
