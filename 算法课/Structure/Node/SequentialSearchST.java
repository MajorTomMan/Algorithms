package Structure.Node;

public class SequentialSearchST<Key,Value>{
    private Node first;
    private class Node{
        Key key;
        Value val;
        Node next;
        public Node(Key key, Value val,Node next) {
            this.key = key;
            this.val = val;
            this.next = next;
        }
    }
    public Value get(Key key){
        for(Node x=first;x!=null;x=x.next){
            if(key.equals(x.key)){
                return x.val; //命中
            }
        }
        return null; //未命中
    }
    public void put(Key key,Value val){
        for(Node x=first;x!=null;x=x.next){
            if(key.equals(x.key)){
                x.val=val;
                return; //命中
            }
        }
        first=new Node(key,val,first); //未命中,新建节点
    }
    public int size() {
        int count=0;
        for(Node x=first;x!=null;x=x.next){
            count++;
        }
        return count;
    }
}
