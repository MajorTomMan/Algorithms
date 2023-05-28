
package sort.structure.linear;

import sort.structure.common.Common;

public class Insertion extends Common{
    @Override
    public void sort(Comparable[] a) {
        // TODO Auto-generated method stub
        int N=a.length;
        for(int i=1;i<N;i++){
            for(int j=i;j>0&&less(a[j],a[j-1]);j--){
                exch(a, j, j-1);
            }
        }
        show(a);
    }
}
