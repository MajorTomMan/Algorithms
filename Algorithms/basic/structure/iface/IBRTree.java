/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 18:03:17
 * @FilePath: /alg/Algorithms/basic/structure/iface/IBRTree.java
 */
package basic.structure.iface;

public interface IBRTree<T> extends Example<T>,ExampleTree<T>{
    T Max();
    T Min();
}
