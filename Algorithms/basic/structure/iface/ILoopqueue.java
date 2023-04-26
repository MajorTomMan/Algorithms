/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 18:03:25
 * @FilePath: /alg/Algorithms/basic/structure/iface/ILoopqueue.java
 */
package basic.structure.iface;


public interface ILoopqueue<T>{
    T dequeue();
    void enqueue(T data);
}
