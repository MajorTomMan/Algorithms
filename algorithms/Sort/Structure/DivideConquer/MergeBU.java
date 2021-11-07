package Sort.Structure.DivideConquer;

import Sort.Structure.Example.Example;

public class MergeBU extends Example{ //自底向上 适合链表形式组织的数据
    private static Comparable[] aux;
    public void sort(Comparable[] a){
        int N=a.length;
        aux=new Comparable[a.length];
        for(int sz=1;sz<N;sz=sz+sz){
            for(int lo=0;lo<N-sz;lo+=sz+sz){
                merge(a, lo, lo+sz-1,Math.min(lo+sz+sz-1,N-1));
            }
        }
    }
    private static void merge(Comparable[] a,int lo,int mid,int hi){
        int i=lo,j=mid+1;
        for(int k=lo;k<=hi;k++){
            aux[k]=a[k];
        }
        for(int k=lo;k<=hi;k++){
            if(i>mid){
                a[k]=aux[j++];
            }
            else if(j>hi){
                a[k]=aux[i++];
            }
            else if(less(aux[j],aux[i])){
                a[k]=aux[j++];
            }
            else{
                a[k]=aux[i++];
            }
        }
    }
}
