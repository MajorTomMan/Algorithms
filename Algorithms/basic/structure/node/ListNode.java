
package basic.structure.node;

public class ListNode<T>{
    public T data;
    public ListNode<T> next;
    public ListNode(){
        
    }
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
