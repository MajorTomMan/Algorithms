package Basic.Structure;

import java.util.Iterator;

import Basic.Structure.Node.twoSideNode;

//双向链表
public class twoSideLink<T> implements Iterable<T> {
    private int size=0; // 链表的长度
    private final int MaxSize = 100; // 链表最长长度
    private twoSideNode<T> head; // 链表头结点
    private twoSideNode<T> last; // 链表尾部结点

    public boolean add(T data) {
        if (isEmpty()) {
            head = new twoSideNode<>(data, null, null);
            last = head;
            size++;
            return true;
        }
        twoSideNode<T> temp = head.next;
        if (temp == null) {
            head.next = new twoSideNode<>(data, null, head);
            size++;
            return true;
        }
        try {
            search(temp, head, data);
            setPrevious(temp);
            return true;
        } catch (Exception error) {
            System.out.println(error);
            return false;
        } // 往头尾部新增结点
    }

    private boolean isEmpty() {
        return head == null;
    }

    private void search(twoSideNode<T> node, twoSideNode<T> previous, T var) {
        if (node.next == null) {
            node.next = new twoSideNode<>(var, null, null);
            last = node.next;
            size++;
            return;
        }
        search(node.next, node, var);
    }

    public void setPrevious(twoSideNode<T> node) {
        setPrevious(node, head);
    }

    private twoSideNode<T> setPrevious(twoSideNode<T> node, twoSideNode<T> previous) {
        if (node == null) {
            return node;
        }
        setPrevious(node.next, node);
        if (previous.data == head.data) {
            return node;
        }
        node.pre = previous;
        return node;
    }

    public boolean add(int index, T data) { // 往指定索引位置的前方插入结点
        twoSideNode<T> temp = head;
        if (isEmpty()) {
            head = new twoSideNode<>(data, null, null);
            last = head;
            size++;
            return true;
        }
        try {
            handleAdd(index, data);
            return true;
        } catch (Exception error) {
            System.err.println(error);
            return false;
        }

    }

    private void handleAdd(int index, T data) {
        handleAdd(head, 0, index, data);
        setPrevious(head.next);
    }

    private void handleAdd(twoSideNode<T> node, int location, int index, T data) {
        if (location == index) {
            twoSideNode<T> rear = node.next;
            node.next = new twoSideNode<>(data, null, null);
            node.next.next = rear;
            size++;
            return;
        }
        handleAdd(node.next, ++location, index, data);
    }

    public boolean remove(int index) { // 移除指定节点
        if (isEmpty()) {
            return false;
        }
        try {
            handleRemove(index);
            return true;
        } catch (Exception error) {
            System.err.println(error);
            return false;
        }
    }

    private void handleRemove(int index) {
        if (index == 0) {
            head = head.next;
            head.pre = null;
            setPrevious(head.next);
            size--;
            return;
        }
        else if(index==size){
            last.pre.next=null;
            last=last.pre;
            size--;
            return;
        }
        handleRemove(head,0, index);

    }

    private void handleRemove(twoSideNode<T> node, int location, int index) {
        if (location == index && index != size) {
            node.next.pre = node.pre;
            node.pre.next = node.next;
            size--;
            return;
        }
        handleRemove(node.next, ++location, index);
    }

    public boolean replace(int index, T data) { // 更改指定结点
        try {
            replace(head,0,index, data);
            return true;
        } catch (Exception e) {
            //TODO: handle exception
            System.out.println(e);
            return false;
        }
    }
    private void replace(twoSideNode<T> node,int location,int index,T data){
        if(location==index){
            node.data=data;
            return;
        }
        replace(node.next, ++location, index, data);
    }

    public T get(int index) { // 返回指定索引位置的元素
        int location=0;
        twoSideNode<T> temp=head;
        while(location!=index&&temp.next!=null){
            temp=temp.next;
            location++;
        }
        return temp.data;
    }

    public boolean find(T data) { // 检索指定元素是否在链表内
        return isExist(head, data);
    }
    private Boolean isExist(twoSideNode<T> node,T data){
        while(node.next!=null){
            if(node.data.equals(data)){
                return true;
            }
            node=node.next;
        }
        return false;
    }

    public int getSize() {
        return this.size;
    }
    public void setSize(int size) {
        this.size = size;
    }

    public int getMaxSize() {
        return MaxSize;
    }

    public twoSideNode<T> getHead() {
        return head;
    }

    public void setHead(twoSideNode<T> head) {
        this.head = head;
    }

    public twoSideNode<T> getLast() {
        return last;
    }
    
    public void setLast(twoSideNode<T> last) {
        this.last = last;
    }

    @Override
    public Iterator<T> iterator() {
        // TODO Auto-generated method stub
        return new LinkIterator();
    }
    private class LinkIterator implements Iterator<T>{
        private twoSideNode<T> current=head;
        @Override
        public boolean hasNext() {
            // TODO Auto-generated method stub
            return current!=null;
        }

        @Override
        public T next() {
            // TODO Auto-generated method stub
            T data=current.data;
            current=current.next;
            return data;
        }
        
    }
}
