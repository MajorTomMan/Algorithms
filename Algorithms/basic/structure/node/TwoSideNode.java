package basic.structure.node;


public class TwoSideNode<T> {
    // 数据域
    public T data; // 用于存储数据的区域
    // 结点域
    public TwoSideNode<T> next; // 指向下一块数据区域的指针
    public TwoSideNode<T> pre; // 指向上一块数据区域的指针

    public TwoSideNode(T data, TwoSideNode<T> next, TwoSideNode<T> pre) { // 构造方法，用于初始化数据区域以及指针
        this.data = data;
        this.next = next;
        this.pre = pre;
    }
    @Override
    public String toString() {
        return "[data=" + data + ", next=" + next + ", pre=" + pre + "]";
    }
}
