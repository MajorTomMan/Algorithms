package sort.structure.linear;

import sort.structure.common.Common;

public class Shell extends Common{
    @Override
    public void sort(Comparable[] a) {
        // TODO Auto-generated method stub
        int N=a.length;
        int h=1;
        while(h<N/3){ //影响因子 递增数列
            h=3*h+1;
        }
        while(h>=1){
            for(int i=h;i<N;i++){
                for(int j=i;j>=h&&less(a[j],a[j-h]);j-=h){
                    exch(a, j, j-h);
                }
            }
            h=h/3;
        }
    }
    
}
