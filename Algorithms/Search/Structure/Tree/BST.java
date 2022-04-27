package Search.Structure.Tree;

import java.util.LinkedList;
import java.util.Queue;

import Search.Structure.Example.OrderSymbolTable;

public class BST<Key extends Comparable<Key>,Value> extends OrderSymbolTable<Key,Value>{
    private Node root;
    private class Node{
        private Key key;
		private Value val;
        private Node left,right;
        private int N;
        public Node(Key key, Value val, int n) {
			this.key = key;
			this.val = val;
			N = n;
		}
    }
    @Override
    public Iterable<Key> keys(Key lo, Key hi) {
        // TODO Auto-generated method stub
        Queue<Key> queue=new LinkedList<Key>();
        keys(root,queue,lo,hi);
        return queue;
    }
    private void keys(Node x, Queue<Key> queue, Key lo, Key hi) {
        if(x==null){
            return;
        }
        int cmplo=lo.compareTo(x.key);
        int cmphi=hi.compareTo(x.key);
        if(cmplo<0){
            keys(x.left,queue,lo,hi);
        }
        if(cmplo<=0&&cmphi>=0){
            queue.add(x.key);
        }
        if(cmphi>0){
            keys(x.right,queue,lo,hi);
        }
	}
	@Override
    public void deleteMin() {
        // TODO Auto-generated method stub
        root=deleteMin(root);
    }
    private Node deleteMin(Node x){
        if(x.left==null){
            return x.right;
        }
        x.left=deleteMin(x.left);
        x.N=size(x.left)+size(x.right)+1;
        return x;
    }
    @Override
    public void deleteMax() {
        // TODO Auto-generated method stub
        root=deleteMax(root);
    }
    private Node deleteMax(Node x){
        if(x.right==null){
            return x.left;
        }
        x.right=deleteMin(x.right);
        x.N=size(x.left)+size(x.right)+1;
        return x;
    }
    @Override
    public int size(){
        // TODO Auto-generated method stub
        return size(root);
    }
    private int size(Node x){
        if(x==null){
            return 0;
        }
        else{
            return x.N;
        }
    }
	@Override
	public Key min() {
		// TODO Auto-generated method stub
		return min(root).key;
	}
    private Node min(Node x){
        if(x.left==null){
            return x;
        }
        return min(x.left);
    }
	@Override
	public Key max() {
		// TODO Auto-generated method stub
		return max(root).key;
	}
    private Node max(Node x){
        if(x.right==null){
            return x;
        }
        return max(x.right);
    }
	@Override
	public Key floor(Key key) {
		// TODO Auto-generated method stub
		Node x=floor(root,key);
        if(x==null){
            return null;
        }
        return x.key;
	}
    private Node floor(Node x,Key key){
        if(x==null){
            return null;
        }
        int cmp=key.compareTo(x.key);
        if(cmp==0){
            return x;
        }
        else if(cmp<0){
            return floor(x.left,key);
        }
        Node t=floor(x.right,key);
        if(t!=null){
            return t;
        }
        else{
            return x;
        }
    }
	@Override
	public Key ceiling(Key key) {
		// TODO Auto-generated method stub
		Node x=ceiling(root,key);
        if(x==null){
            return null;
        }
        return x.key;
	}
    private Node ceiling(Node x,Key key){
        if(x==null){
            return null;
        }
        int cmp=key.compareTo(x.key);
        if(cmp==0){
            return x;
        }
        else if(cmp>0){
            return ceiling(x.right,key);
        }
        Node t=ceiling(x.left,key);
        if(t!=null){
            return t;
        }
        else{
            return x;
        }
    }
	@Override
	public int rank(Key key) {
		// TODO Auto-generated method stub
		return rank(key,root);
	}
    private int rank(Key key,Node x){
        if(x==null){
            return 0;
        }
        int cmp=key.compareTo(x.key);
        if(cmp<0){
            return rank(key,x.left);
        }
        else if(cmp>0){
            return 1+size(x.left)+rank(key,x.right);
        }
        else{
            return size(x.left);
        }
    }
	@Override
	public Key select(int k) {
		// TODO Auto-generated method stub
		return select(root,k).key;
	}
    private Node select(Node x,int k){
        if(x==null){
            return null;
        }
        int t=size(x.left);
        if(t>k){
            return select(x.left, k);
        }
        else if(t<k){
            return select(x.right,k-t-1);
        }
        else{
            return x;
        }
    }
	@Override
	public void delete(Key key) {
		// TODO Auto-generated method stub
        root=delete(root,key);
	}
    private Node delete(Node x,Key key){
        if(x==null){
            return null;
        }
        int cmp=key.compareTo(x.key);
        if(cmp<0){
            x.left=delete(x.left, key);
        }
        else if(cmp<0){
            x.right=delete(x.right, key);
        }
        else{
            if(x.right==null){
                return x.left;
            }
            if(x.left==null){
                return x.right;
            }
            Node t=x;
            x=min(t.right);
            x.right=deleteMin(t.right);
            x.left=t.left;
        }
        x.N=size(x.left)+size(x.right)+1;
        return x;
    }
	@Override
	public void put(Key key, Value val) {
		// TODO Auto-generated method stub
		root=put(root,key,val);
	}
    private Node put(Node x,Key key,Value val){
        if(x==null){
            return new Node(key,val,1);
        }
        int cmp=key.compareTo(x.key);
        if(cmp<0){
            x.left=put(x.left,key,val);
        }
        else if(cmp>0){
            x.right=put(x.right,key,val);
        }
        else{
            x.val=val;
        }
        x.N=size(x.left)+size(x.right)+1;
        return x;
    }
	@Override
	public Value get(Key key) {
		// TODO Auto-generated method stub
		return get(root,key);
	}
    private Value get(Node x,Key key){
        if(x==null){
            return null;
        }
        int cmp=key.compareTo(x.key);
        if(cmp<0){
            return get(x.left,key);
        }
        else if(cmp>0){
            return get(x.right,key);
        }
        else{
            return x.val;
        }
    }
}
