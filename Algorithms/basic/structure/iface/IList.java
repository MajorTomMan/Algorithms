/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 18:03:22
 * @FilePath: /alg/Algorithms/basic/structure/iface/IList.java
 */
package basic.structure.iface;
public interface IList<T> extends Example<T>{
    public void Delete(int index);
    public void Insert(T data);
    public void Reverse();
}
