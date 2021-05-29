 package Structure;


public class Linkedlist<T> implements ILinkedlist<T>{
    private Node<T> head;
    public void Initial(Node<T> head) {
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

    public void Insert(Node<T> node) {
        if(head==null){
            head=node;
            return;
        }
        Node<T> temp=head;
        while(temp.next!=null){
            temp=temp.next;
        }
        temp.next=node;
    }

    public void InsertMiddle(int index, Node<T> node) {
        if(head==null){
            return;
        }
        Node<T> temp=head;
        int i=0;
        while(i!=index){
            temp=temp.next;
            i++;
        }
        node.next=temp.next;
        temp.next=node;
    }

    public void InsertHead(Node<T> node) {
        if(head==null){
            head=node;
            return;
        }
        node.next=head.next;
        head.next=node;
    }
    public void Reverse(Linkedlist<T> list){
        ;
    }
    public void Show(Linkedlist<T> list) {
        Node<T> temp=list.head;
        while(temp!=null){
            System.out.println(temp.data.saveData);
            temp=temp.next;
        }
    }
}
