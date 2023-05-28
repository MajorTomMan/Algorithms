
package sort.structure.divideconquer;

import func.random.StdRandom;
import sort.structure.common.Common;

public class Quick extends Common{
    @Override
    public void sort(Comparable[] a) {
        // TODO Auto-generated method stub
        StdRandom.shuffle(a);
        sort(a,0,a.length-1);
        show(a);
    }
    public static void sort(Comparable[] a,int lo,int hi) {
        if(hi<=lo){
            return;
        }
        int j=partition(a,lo,hi);
        sort(a, lo, j-1);
        sort(a, j+1, hi);
    }
    public static int partition(Comparable[] a,int lo,int hi){
        int i=lo,j=hi+1;
        Comparable v=a[lo];
        while(true){
            while(less(a[++i],v)){
                if(i==hi){
                    break;
                }
            }
            while(less(v,a[--j])){
                if(j==lo){
                    break;
                }
            }
            if(i>=j){
                break;
            }
            exch(a, i, j);
        }
        exch(a, lo, j);
        return j;
    }
}
