 package Structure;


public class Linkedlist<T> implements ILinkedlist<T>{
    private Node<T> head;
    public void Initial(T var) {
        Node<T> head=new Node<>();
        createNode(head);
        head.data.saveData=var;
        this.head = head;
    }

    public void Delete(int index) {
        int i=0;
        Node<T> temp=head;
        Node<T> pre=new Node<T>();
        while(i!=index&&index!=0){
            pre=temp;
            temp=temp.next;
            i++;
        }
        pre.next=temp.next;
    }
    public void delete(T var){
        Node<T> temp=head;
        Node<T> pre=new Node<T>();
        boolean flag=false;
        while(temp.data.saveData!=var&&temp!=null){
            if(temp.data.saveData.equals(var)){
                flag=true;
                break;
            }
            pre=temp;
            temp=temp.next;
        }
        if(flag){
            pre.next=temp.next;
            temp=null;
        }
        else{
            System.out.println("未找到数据");
        }
    }

    public void Insert(T var) {
        if(head==null){
            Initial(var);
            return;
        }
        Node<T> temp=head;
        Node<T> node=new Node<>();
        createNode(node);
        node.data.saveData=var;
        while(temp.next!=null){
            temp=temp.next;
        }
        temp.next=node;
    }

    public void InsertMiddle(int index,T var) {
        if(head==null){
            Initial(var);
            return;
        }
        Node<T> temp=head;
        Node<T> node=new Node<>();
        createNode(node);
        node.data.saveData=var;
        int i=0;
        while(i!=index){
            temp=temp.next;
            i++;
        }
        node.next=temp.next;
        temp.next=node;
    }

    public void InsertHead(T var) {
        if(head==null){
            Initial(var);
            return;
        }
        Node<T> node=new Node<>();
        createNode(node);
        node.data.saveData=var;
        node.next=head.next;
        head.next=node;
    }
    public void Reverse(Linkedlist<T> list){
        Node<T> beg=null,mid=head,end=head.next;
        while(end!=null){
            mid.next=beg;
            beg=mid;
            mid=end;
            end=end.next;
        }
        mid.next=beg;
        head=mid;
    }
    public void Show(Linkedlist<T> list) {
        Node<T> temp=list.head;
        while(temp!=null){
            System.out.println(temp.data.saveData);
            temp=temp.next;
        }
    }
    public void show(Node<T> node){
        if(node==null){
            return;
        }
        show(node.next);
        System.out.println(node.data.saveData);
    }
    public Node<T> getHead() {
        return head;
    }

    public void setHead(Node<T> head) {
        this.head = head;
    }
    private void createNode(Node<T> node){
        Data<T> data=new Data<>();
        node.data=data;
    }

}
