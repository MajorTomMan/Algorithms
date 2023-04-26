/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 18:03:33
 * @FilePath: /alg/Algorithms/basic/structure/iface/IStack.java
 */
package basic.structure.iface;


public interface IStack<T> extends Example<T>{
    T pop();
    void push(T data);
}