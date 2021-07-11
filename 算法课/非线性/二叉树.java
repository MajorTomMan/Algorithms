package 非线性;
import Structure.BTree;
import Structure.Node.BTnode;
import Structure.Node.Data;

public class 二叉树 {
    public static void main(String[] args) {
        int i=0;
        String str="It was the best of times it was the worst of times it was the age of wisdom";
        String[] temp=str.split(" ");
        String[] choose={"left","right"};
        BTree<String> bTree=new BTree<>();
        while(i!=temp.length){
            BTnode<String> node=new BTnode<>(new Data<String>(temp[i]),null,null);
            bTree.Insert(node,choose[i%2]);
            i++;
        }
        bTree.SearchAll_M(bTree.getRoot());
    }
}
