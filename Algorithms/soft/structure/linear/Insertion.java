/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:43:58
 * @FilePath: /alg/Algorithms/soft/structure/linear/Insertion.java
 */
package soft.structure.linear;

import soft.structure.example.Example;

public class Insertion extends Example{
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
