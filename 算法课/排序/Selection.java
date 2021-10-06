package 排序;

import java.lang.Comparable;

public class Selection extends Example{
    public void sort(Comparable[] a){
        int N=a.length;
        for(int i=0;i<N;i++){
            int min=i;
            for(int j=i+1;j<N;j++){
                if(less(a[j],a[min])){
                    min=j;
                }
            }
            exch(a, i, min);
        }
        show(a);
    }
}
