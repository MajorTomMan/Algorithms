package 非线性;


import java.util.Random;

import Structure.BRTree;
import Structure.Node.BTnode;
import Structure.Node.Data;


public class 二叉搜索树{
    public static void main(String[] args) {
        int i=0;
        BRTree<Integer> bTree=new BRTree<Integer>();
        Random random=new Random();
        System.out.println("-----------------------raw data--------------------");
        while(i!=12){
            BTnode<Integer> node=new BTnode<Integer>(new Data<Integer>(random.nextInt(100)),null,null);
            System.out.print(node.item.saveData+",");
            bTree.Insert(node);
            i++;
        }
        System.out.println();
        System.out.println("---------------------------------------------------");
        System.out.println(bTree.FindMax(bTree.getRoot()).item.saveData);
        System.out.println(bTree.FindMin(bTree.getRoot()).item.saveData);
        System.out.println("-----------FindMax&&FindMin No Feedback---------------");
        System.out.println(bTree.FindMax_NoFeedback(bTree.getRoot()).item.saveData);
        System.out.println(bTree.FindMin_NoFeedback(bTree.getRoot()).item.saveData);
        System.out.println("-----------SearchAll_P---------------");
        bTree.SearchAll_P(bTree.getRoot());
        System.out.println("-----------SearchAll_M---------------");
        bTree.SearchAll_M(bTree.getRoot());
        System.out.println("-----------SearchAll_R---------------");
        bTree.SearchAll_R(bTree.getRoot());
        System.out.println("-----------SearchAll_P_noFeedback---------------");
        bTree.SearchAll_P_noFeedBack(bTree.getRoot());
        System.out.println("-----------SearchAll_M_noFeedback---------------");
        bTree.SearchAll_M_noFeedBack(bTree.getRoot());
        System.out.println("-----------SearchAll_R_noFeedback---------------");
        bTree.SearchAll_R_noFeedBack(bTree.getRoot());
        System.out.println("-----------SearchAll_L_noFeedback---------------");
        bTree.SearchAll_L_noFeedBack(bTree.getRoot());
        System.out.println("------------------------------------------------");
        System.out.println(bTree.BRTreeItemCount());
        System.out.println("------------------------------------------------");
        BTnode<Integer> node=new BTnode<Integer>(new Data<Integer>(50),null,null);
        bTree.Insert(node);
        bTree.Delete(50);
        bTree.SearchAll_M(bTree.getRoot());
        System.out.println("------------------------------------------------");
    }
}
