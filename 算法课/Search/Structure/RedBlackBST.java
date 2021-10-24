package Search.Structure;

import java.util.Queue;
import java.util.LinkedList;

public class RedBlackBST<Key extends Comparable<Key>,Value> extends OrderSymbolTable<Key,Value>{
    private RBTNode root;
    private static final boolean RED=true;
    private static final boolean BLACK=false;
    private int i=0;
    private class RBTNode{
        Key key; //键
        Value val; //值
        RBTNode left,right; //左右子树
        int N; //子树中的节点总数
        boolean color; //由其父节点指向它的连接颜色
        RBTNode(Key key, Value val, int N, Boolean color) {
            this.key = key;
            this.val = val;
            this.N = N;
            this.color = color;
        }
    }
    private boolean isRed(RBTNode x){
        if(x==null){
            return false;
        }
        return x.color==RED;
    }
    @Override
    public void delete(Key key) {
        // TODO Auto-generated method stub
        if(!isRed(root.left)&&!isRed(root.right)){
            root.color=RED;
        }
        root=delete(root,key);
        if(!isEmpty()){
            root.color=BLACK;
        }
    }
    private RBTNode delete(RBTNode x,Key key){
        if(key.compareTo(x.key)<0){
            if(!isRed(x.left)&&!isRed(x.left.left)){
                x=moveRedLeft(x);
            }
            x.left=delete(x.left,key);
        }
        else{
            if(!isRed(x.left)){
                x=rotateRight(x);
            }
            if(key.compareTo(x.key)==0&&x.right==null){
                return null;
            }
            if(!isRed(x.right)&&!isRed(x.right.left)){
                x=moveRedRight(x);
            }
            if(key.compareTo(x.key)==0){
                x.val=get(x.right,min(x.right).key);
                x.key=min(x.right).key;
                x.right=deleteMin(x.right);
            }
            else{
                x.right=delete(x.right, key);
            }
        }
        return balance(x);
    }
    @Override
    public void deleteMax() {
        // TODO Auto-generated method stub
        if(!isRed(root.left)&&!isRed(root.right)){
            root.color=RED;
        }
        root=deleteMax(root);
        if(!isEmpty()){
            root.color=BLACK;
        }
    }
    private RBTNode deleteMax(RBTNode x) {
        // TODO Auto-generated method stub
        if(isRed(x.left)){
            x=rotateRight(x);
        }
        if(x.right==null){
            return null;
        }
        if(!isRed(x.right)&&!isRed(x.right.left)){
            x=moveRedRight(x);
        }
        x.right=deleteMax(x.right);
        return balance(x);
    }
    private RBTNode moveRedRight(RBTNode x){
        flipColors(x);
        if(!isRed(x.left.left)){
            x=rotateRight(x);
        }
        return x;
    }
    private RBTNode moveRedLeft(RBTNode x){
        flipColors(x);
        if(isRed(x.right.left)){
            x.right=rotateRight(x.right);
            x=rotateLeft(x);
        }
        return x;
    }
    @Override
    public void deleteMin() {
        // TODO Auto-generated method stub
        if(isRed(root.left)&&!isRed(root.right)){
            root.color=RED;
        }
        root=deleteMin(root);
        if(!isEmpty()){
            root.color=BLACK;
        }
    }
    private RBTNode deleteMin(RBTNode x) {
        // TODO Auto-generated method stub
        if(x.left==null){
            return null;
        }
        if(!isRed(x.left)&&!isRed(x.left.left)){
            x=moveRedLeft(x);
        }
        x.left=deleteMin(x.left);
        return balance(x);
    }
    private RBTNode balance(RBTNode x){
        if(isRed(x.right)){
            x=rotateLeft(x);
        }
        if(isRed(x.right)&&!isRed(x.left)){
            x=rotateLeft(x);
        }
        if(isRed(x.left)&&isRed(x.left.left)){
            x=rotateRight(x);
        }
        if(isRed(x.left)&&!isRed(x.right)){
            flipColors(x);
        }
        x.N=size(x.left)+size(x.right)+1;
        return x;
    }
    private RBTNode rotateLeft(RBTNode h){
        RBTNode x=h.right;
        h.right=x.left;
        x.left=h;
        x.color=h.color;
        h.color=RED;
        x.N=h.N;
        h.N=1+size(h.left)+size(h.right);
        return x;
    }
    private RBTNode rotateRight(RBTNode h){
        RBTNode x=h.left;
        h.left=x.right;
        x.right=h;
        x.color=h.color;
        h.color=RED;
        x.N=h.N;
        h.N=1+size(h.left)+size(h.right);
        return x;
    }
    private void flipColors(RBTNode h){
        h.color=RED;
        h.left.color=BLACK;
        h.right.color=BLACK;
    }
    public int size(){
        // TODO Auto-generated method stub
        return size(root);
    }
    private int size(RBTNode x){
        if(x==null){
            return 0;
        }
        else{
            return x.N;
        }
    }
    @Override
    public Iterable<Key> keys() {
        // TODO Auto-generated method stub
        return super.keys();
    }
    @Override
    public void put(Key key, Value val) {
        // TODO Auto-generated method stub
        root=put(root, key, val);
        root.color=BLACK;
    }
    private RBTNode put(RBTNode x,Key key, Value val) {
        // TODO Auto-generated method stub
        if(x==null){
            return new RBTNode(key,val,1,RED);
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
        if(isRed(x.right)&&!isRed(x.left)){
            x=rotateLeft(x);
        }
        if(isRed(x.left)&&isRed(x.left.left)){
            x=rotateRight(x);
        }
        if(isRed(x.left)&&isRed(x.right)){
            flipColors(x);
        }
        x.N=size(x.left)+size(x.right)+1;
        return x;
    }
    @Override
	public Key min() {
		// TODO Auto-generated method stub
		return min(root).key;
	}
    private RBTNode min(RBTNode x){
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
    private RBTNode max(RBTNode x){
        if(x.right==null){
            return x;
        }
        return max(x.right);
    }
	@Override
	public Key floor(Key key) {
		// TODO Auto-generated method stub
		RBTNode x=floor(root,key);
        if(x==null){
            return null;
        }
        return x.key;
	}
    private RBTNode floor(RBTNode x,Key key){
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
        RBTNode t=floor(x.right,key);
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
		RBTNode x=ceiling(root,key);
        if(x==null){
            return null;
        }
        return x.key;
	}
    private RBTNode ceiling(RBTNode x,Key key){
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
        RBTNode t=ceiling(x.left,key);
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
    private int rank(Key key,RBTNode x){
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
    private RBTNode select(RBTNode x,int k){
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
    public Iterable<Key> keys(Key lo, Key hi) {
        // TODO Auto-generated method stub
        Queue<Key> queue=new LinkedList<Key>();
        keys(root,queue,lo,hi);
        return queue;
    }
    private void keys(RBTNode x, Queue<Key> queue, Key lo, Key hi) {
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
	public Value get(Key key) {
		// TODO Auto-generated method stub
		return get(root,key);
	}
    private Value get(RBTNode x,Key key){
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
