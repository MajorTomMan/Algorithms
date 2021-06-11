package 非线性;
import Structure.Data;
import Structure.TRnode;
import Structure.Tree;

public class 树 {
    public static void main(String[] args) {
        int i=0;
        String str="It was the best of times, it was the worst of times, it was the age of wisdom, it was the age of foolishness, it was the epoch of belief, it was the epoch of incredulity, it was the season of Light, it was the season of Darkness, it was the spring of hope, it was the winter of despair, we had everything before us, we had nothing before us, we were all going direct to Heaven, we were all going direct the other way—in short, the period was so far like the present period, that some of its noisiest authorities insisted on its being received, for good or for evil, in the superlative degree of comparison only.";
        String[] temp=str.split(", ");
        Tree<String> tree=new Tree<>();
        while(i!=temp.length){
            TRnode<String> node=new TRnode<>();
            Data<String> data=new Data<>();
            node.data=data;
            node.data.saveData=temp[i];
            tree.Insert(node);
            i++;
        }
        System.out.println(tree.TreeItemCount());
        tree.Show(tree.getRoot());
    }
}
