package 非线性;

import Structure.Tree;
import java.util.LinkedList;
import java.util.Random;

import Structure.Data;
import Structure.TRnode;
public class 树 {
    public static void main(String[] args) {
        int i=0;
        Tree<String> tree=new Tree<>();
        TRnode<String> next=null;
        String str="It was the best of times, it was the worst of times, it was the age of wisdom, it was the age of foolishness, it was the epoch of belief, it was the epoch of incredulity, it was the season of Light, it was the season of Darkness, it was the spring of hope, it was the winter of despair, we had everything before us, we had nothing before us, we were all going direct to Heaven, we were all going direct the other way—in short, the period was so far like the present period, that some of its noisiest authorities insisted on its being received, for good or for evil, in the superlative degree of comparison only.";
        String[] str_t=str.split(", ");
        i=0;
        while(i<str_t.length){
            TRnode<String> node=new TRnode<>();
            createNode(node);
            node.data.saveData=str_t[i];
            if(i%5==0&&i!=0){
                next=tree.getRoot().child.get(i/(i+3));
                tree.Insert(node,next);
            }
            else if(next!=null){
                tree.Insert(node,next);
            }
            else{
                tree.Insert(node,tree.getRoot());
            }
            i++;
        }
        i=0;
        while(i!=3){
            Random random=new Random();
            int j=random.nextInt(6);
            if(tree.Search(str_t[j])==null){
                System.out.println("未找到相关数据,无法删除");
            }
            else{
                TRnode<String> delchild=tree.Search(str_t[j]);
                tree.Delete(delchild,delchild.father);
            }
            i++;
        }
    }
    private static void createNode(TRnode<String> node){
        node.data=new Data<>();
        node.child=new LinkedList<>();
    }
}
