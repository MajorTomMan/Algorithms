/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 18:03:19
 * @FilePath: /alg/Algorithms/basic/structure/iface/ICycleList.java
 */
package basic.structure.iface;

public interface ICycleList<T> extends Example<T> {
    public void Initial(T data);
    public void Delete(int index);
    public void Insert(T data);
}
