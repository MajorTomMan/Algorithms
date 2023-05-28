
package sort.structure.divideconquer;

import sort.structure.common.Common;

public class Merge extends Common{ //自顶向下
    private static Comparable[] aux;
    public void sort(Comparable[] a){
        aux=new Comparable[a.length];
        sort(a,0,a.length-1);
    }
    private void sort(Comparable[] a,int lo,int hi){
        if(hi<=lo){
            return;
        }
        int mid=lo+(hi-lo)/2; //确定中位数用来拆分数组
        sort(a,lo,mid); //对左边排序
        sort(a,mid+1,hi); //对右边排序
        merge(a,lo,mid,hi); //归并
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
