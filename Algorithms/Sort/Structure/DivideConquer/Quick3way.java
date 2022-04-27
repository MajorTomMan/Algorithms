package Sort.Structure.DivideConquer;

import Func.Random.StdRandom;
import Sort.Structure.Example.Example;

public class Quick3way extends Example{//适用于需要处理大量重复数据的数组,其效率为线性级别,快排是对数级别
    @Override
    public void sort(Comparable[] a) {
        // TODO Auto-generated method stub
        StdRandom.shuffle(a);
        sort(a,0,a.length-1);
    }
    private static void sort(Comparable[] a,int lo,int hi) {
        if(hi<=lo){
            return;
        }
        int it=lo,i=lo+1,gt=hi;
        Comparable v=a[lo];
        while(i<=gt){
            int cmp=a[i].compareTo(v);
            if(cmp<0){
                exch(a, it++, i++);
            }
            if(cmp>0){
                exch(a,i,gt--);
            }
            else{
                i++;
            }
        }
        sort(a, lo, it-1);
        sort(a, gt+1,  hi);
    }
}
