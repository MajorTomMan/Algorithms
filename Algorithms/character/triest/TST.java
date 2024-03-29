package character.triest;

public class TST<Value>{ //三向单词查找树
    private Node root;
    private class Node{
        char c;
        Value val;
        Node left,mid,right;
    }
    public Value get(String key){
        Node x=get(root,key,0);
        if(x==null){
            return null;
        }
        return (Value) x.val;
    }
    private Node get(Node x,String key,int d){
        //返回以x作为根节点的子单词查找树中和key相关的值
        if(x==null){
            return null;
        }
        char c=key.charAt(d);
        if(c<x.c){
            return get(x.left,key,d);
        }
        else if(c>x.c){
            return get(x.right,key,d);
        }
        else if(d<key.length()-1){
            return get(x.mid,key, d+1);
        }
        else{
            return x;
        }
    }
    public void put(String key,Value val){
        root=put(root,key,val,0);
    }
    private Node put(Node x, String key, Value val, int d) {
        //如果key存在于在x为根节点的子单词查找树中则更新与它相关的值
       char c=key.charAt(d);
       if(x==null){
           x=new Node();
           x.c=c;
       }
       if(c<x.c){
           x.left=put(x.left, key, val, d);
       }
       if(c>x.c){
           x.right=put(x.right, key, val, d);
       }
       else if(d<key.length()-1){
           x.mid=put(x.mid,key,val,d+1);
       }
       else{
           x.val=val;
       }
       return x;
    }
    
}
