/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:43:57
 * @FilePath: /alg/Algorithms/soft/structure/linear/Selection.java
 */
package soft.structure.linear;

import java.lang.Comparable;

import soft.structure.example.Example;

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
    }
}
