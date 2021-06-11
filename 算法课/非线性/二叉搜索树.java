package 非线性;


import java.util.Random;

import Structure.BRTree;
import Structure.BTnode;
import Structure.Data;


public class 二叉搜索树{
    public static void main(String[] args) {
        int i=0;
        BRTree<Integer> bTree=new BRTree<Integer>();
        Random random=new Random();
        System.out.println("-----------------------raw data--------------------");
        while(i!=12){
            BTnode<Integer> node=new BTnode<Integer>();
            Data<Integer> data=new Data<Integer>();
            data.saveData=random.nextInt(100);
            System.out.print(data.saveData+",");
            node.item=data;
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
        BTnode<Integer> node=new BTnode<Integer>();
        Data<Integer> data=new Data<Integer>();
        data.saveData=50;
        node.item=data;
        bTree.Insert(node);
        bTree.Delete(50);
        bTree.SearchAll_M(bTree.getRoot());
        System.out.println("------------------------------------------------");
    }
}
