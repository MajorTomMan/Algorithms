package Structure.Node;

public class SequentialSearchST<Key,Value extends Comparable<Value>>{
    private Node first;
    public class Node{
        Key key;
        Value val;
        Node next;
        public Node(Key key, Value val,Node next) {
            this.key = key;
            this.val = val;
            this.next = next;
        }
        public Key getKey() {
            return key;
        }
        public void setKey(Key key) {
            this.key = key;
        }
        public Value getVal() {
            return val;
        }
        public void setVal(Value val) {
            this.val = val;
        }
        public Node getNext() {
            return next;
        }
        public void setNext(Node next) {
            this.next = next;
        }
    }
    public Node getFirst() {
        return first;
    }
    public Value get(Key key){
        for(Node x=first;x!=null;x=x.next){
            if(key.equals(x.key)){
                return x.val; //命中
            }
        }
        return null; //未命中
    }
    public void put(Key key,Value val){
        for(Node x=first;x!=null;x=x.next){
            if(key.equals(x.key)){
                x.val=val;
                return; //命中
            }
        }
        first=new Node(key,val,first); //未命中,新建节点
    }
    public int size() {
        int count=0;
        for(Node x=first;x!=null;x=x.next){
            count++;
        }
        return count;
    }
    public void delete(Key key){
        Node temp=null;
        for(Node x=first;x!=null;x=x.next){
            if(key.equals(x.key)){
                temp.next=x.next;
                x=null;
            }
            temp=x;
        }
    }
    public void keys(Node head){
        for(Node x=head;x!=null;x=x.next){
            System.out.print(x.key+" ");
        }
    }
    public void sort(Node head){
        if(head==null||head.next==null){
            return;
        }
        Node mid = getMid(head);
        Node right = mid.next;
        mid.next = null;
        //合并
        sort(head);
        sort(right);
        mergeSort(head,right);
    }
    private Node getMid(Node head) {
        if (head == null || head.next == null) {
            return head;
        }
        //快慢指针
        Node slow = head, quick = head;
        //快2步，慢一步
        while (quick.next != null && quick.next.next != null) {
            slow = slow.next;
            quick = quick.next.next;
        }
        return slow;
    }
    private Node mergeSort(Node head1, Node head2) {
        Node p1 = head1, p2 = head2,head;
       //得到头节点的指向
        if (head1.val.compareTo(head2.val)<=0) {
            head = head1;
            p1 = p1.next;
        } else {
            head = head2;
            p2 = p2.next;
        }
    
        Node p = head;
        //比较链表中的值
        while (p1 != null && p2 != null) {
    
            if (p1.val.compareTo(p2.val)<=0) {
                p.next = p1;
                p1 = p1.next;
                p = p.next;
            } else {
                p.next = p2;
                p2 = p2.next;
                p = p.next;
            }
        }
        //第二条链表空了
        if (p1 != null) {
            p.next = p1;
        }
        //第一条链表空了
        if (p2 != null) {
            p.next = p2;
        }
        return head;
    }
}
