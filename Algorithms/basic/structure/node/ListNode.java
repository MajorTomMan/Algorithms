/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:29:19
 * @FilePath: /alg/Algorithms/Basic/Structure/Node/ListNode.java
 */
package basic.structure.node;

public class ListNode<T>{
    public T data;
    public ListNode<T> next;
    public ListNode(T data, ListNode<T> next) {
        this.data = data;
        this.next = next;
    }
    public ListNode(T data) {
        this.data = data;
        this.next = null;
    }
    @Override
    public String toString() {
        return "data:" + data +"->" + next + " ";
    }
}
