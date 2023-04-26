/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:43:50
 * @FilePath: /alg/Algorithms/soft/structure/pq/OrderedArrayMaxPQ.java
 */
package soft.structure.pq;

import func.io.StdOut;

/******************************************************************************
 *  Compilation:  javac OrderedArrayMaxPQ.java
 *  Execution:    java OrderedArrayMaxPQ
 *  Dependencies: StdOut.java 
 *  
 *  Priority queue implementation with an ordered array.
 *
 *  Limitations
 *  -----------
 *   - no array resizing 
 *   - does not check for overflow or underflow.
 *  
 *
 ******************************************************************************/

public class OrderedArrayMaxPQ<Key extends Comparable<Key>> { //有序数组最大优先队列
    private Key[] pq;          // elements
    private int n;             // number of elements

    // set inititial size of heap to hold size elements
    public OrderedArrayMaxPQ(int capacity) {
        pq = (Key[]) (new Comparable[capacity]);
        n = 0;
    }


    public boolean isEmpty() { return n == 0;  }
    public int size()        { return n;       } 
    public Key delMax()      { return pq[--n]; }

    public void insert(Key key) {
        int i = n-1;
        while (i >= 0 && less(key, pq[i])) {
            pq[i+1] = pq[i];
            i--;
        }
        pq[i+1] = key;
        n++;
    }



   /***************************************************************************
    * Helper functions.
    ***************************************************************************/
    private boolean less(Key v, Key w) {
        return v.compareTo(w) < 0;
    }

   /***************************************************************************
    * Test routine.
    ***************************************************************************/
    public static void main(String[] args) {
        OrderedArrayMaxPQ<String> pq = new OrderedArrayMaxPQ<String>(10);
        pq.insert("this");
        pq.insert("is");
        pq.insert("a");
        pq.insert("test");
        while (!pq.isEmpty())
            StdOut.println(pq.delMax());
    }

}
