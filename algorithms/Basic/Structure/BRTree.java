package Basic.Structure;

import Basic.Structure.Interface.IBRTree;
import Basic.Structure.Node.BTnode;
import Basic.Structure.Node.Data;

public class BRTree<T extends Comparable<T>>implements IBRTree<T>{
    private BTnode<T> Root;
    private int depth;
    private int count;
    public BRTree(T data) {
        Root = new BTnode<>(new Data<T>(data),null,null);
    }
    @Override
    public boolean IsEmpty() {
        // TODO Auto-generated method stub
        if(Root==null){
            return true;
        }
        return false;
    }
    @Override
    public int Count() {
        // TODO Auto-generated method stub
        count=Count(Root);
        return count;
    }
    private int Count(BTnode<T> node){
        if(node==null){
            return 0;
        }
        Count(node.Left);
        Count(node.Right);
        return count++;
    }
    @Override
    public void Insert(T data) {
    // TODO Auto-generated method stub
        BTnode<T> node=new BTnode<T>(new Data<T>(data),null,null);
        Insert(Root,node);
    }
    private BTnode<T> Insert(BTnode<T> node,BTnode<T> data){
        if(node==null){
            node=data;
            depth++;
            return node;
        }
        if(node.item.saveData.compareTo(data.item.saveData)<0){ //node<data
            node.Right=Insert(node.Right,data);
            node.SubTreeNum++;
        }
        else if(node.item.saveData.compareTo(data.item.saveData)==0){ //node==data
            return node;
        }
        else if(node.item.saveData.compareTo(data.item.saveData)>0){ //node>data
            node.Left=Insert(node.Left, data);
            node.SubTreeNum++;
        }
        return node;
    }
    @Override
    public void Delete(T data) {
        // TODO Auto-generated method stub
        BTnode<T> node=new BTnode<T>(new Data<T>(data),null,null);
        Delete(Root,node);
    }
    private BTnode<T> Delete(BTnode<T> node,BTnode<T> target){
        if(node==null){
            depth--;
            return node;
        }
        if(node.item.saveData.compareTo(target.item.saveData)<0){
            node.Right=Delete(node.Right,target);
        }
        else if(node.item.saveData.compareTo(target.item.saveData)==0){
            int flag=Check(node, target);
            if(flag==1){
                node=node.Right;
            }
            else if(flag==0){
                node=node.Left;
            }
            else{
                BTnode<T> temp=node;
                node=Min(temp.Right);
                node.Right=DelMin(temp.Right);
                node.Left=temp.Left;
                if(target.item.saveData.compareTo(Root.item.saveData)==0){
                    Root=node;
                }
            }
        }
        else{
            node.Left=Delete(node.Left,target);
        }
        return node;
    }
    private int Check(BTnode<T> node,BTnode<T> target){
        int flag=0;
        if(node.Left==null&&node.Right!=null){
            flag=1;
        }
        else if(node.Right==null&&node.Left!=null){
            flag=0;
        }
        else if(node.Left!=null&&node.Right!=null){
            flag=-1;
        }
        return flag;
    }
    @Override
    public T Max(){
        // TODO Auto-generated method stub
        BTnode<T> node;
        for(node=Root;node.Right!=null;node=node.Right);
        return node.item.saveData;
    }
    private BTnode<T> Max(BTnode<T> node){
        if(node==null){
            return node;
        }
        return Max(node.Right);
    }
    @Override
    public T Min(){
        // TODO Auto-generated method stub
        BTnode<T> node;
        for(node=Root;node.Left!=null;node=node.Left);
        return node.item.saveData;
    }
    private BTnode<T> DelMax(BTnode<T> node){
        if(node.Right==null){
            node.SubTreeNum--;
            return node.Left;
        }
        node.Right=DelMin(node.Right);
        return node;
    }
    private BTnode<T> DelMin(BTnode<T> node){
        if(node.Left==null){
            node.SubTreeNum--;
            return node.Right;
        }
        node.Left=DelMin(node.Left);
        return node;
    }
    private BTnode<T> Min(BTnode<T> node){
        if(node.Left==null){
            return node;
        }
        return Min(node.Left);
    }
    /* 中序遍历 递归 */
    @Override
    public void Show() {
        // TODO Auto-generated method stub
        Show(Root);
    }
    private void Show(BTnode<T> node){
        if(node==null){
            return;
        }
        Show(node.Left);
        System.out.print(node.item.saveData+" ");
        Show(node.Right);
    }
    public int Depth() {
        return depth;
    }
}