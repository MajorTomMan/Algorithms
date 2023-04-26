package soft.structure.pq;


public abstract class ExamplePQ<Key extends Comparable<Key>>{ //优先队列范例
    protected Key[] pq;
    protected int N=0;
    protected abstract void insert(Key v);
    protected abstract Key delMax();
    protected void MaxPQ(int maxN){
        pq=(Key[])new Comparable[maxN+1];
    }
    protected Boolean isEmpty() {
        return N==0;
    }
    protected int size(){
        return N;
    }
    protected boolean less(int i,int j){ //比较谁小
        return pq[i].compareTo(pq[j])<0;
    }
    protected void exch(int i,int j){
        Key t=pq[i];
        pq[i]=pq[j];
        pq[j]=t;
    }
    protected void swim(int k){
        while(k>1&&less(k/2,k)){
            exch(k/2, k);
            k=k/2;
        }
    }
    protected void sink(int k){
        while(2*k<=N){
            int j=2*k;
            if(j<N&&less(j, j+1)){
                j++;
            }
            if(!less(k,j)){
                break;
            }
            exch(k, j);
            k=j;
        }
    }
}
