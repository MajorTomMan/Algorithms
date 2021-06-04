package Structure;

public class Cyclelist<T> implements ICyclelist<T> {
    private Node<T> head;
    @Override
    public void Initial(T var) {
        // TODO Auto-generated method stub
        Node<T> node=new Node<>();
        createNode(node);
        node.data.saveData=var;
        head=node;
        node.next=head;
    }

    @Override
    public void Delete(int index) {
        // TODO Auto-generated method stub
        int i=0;
        if(head==null){
            return;
        }
        Node<T> temp=head;
        Node<T> pre=new Node<>();
        while(i!=index){
            pre=temp;
            temp=temp.next;
            i++;
        }
        head=temp.next;
        pre.next=head;
        System.out.println("被删除的节点是:"+temp.data.saveData);
    }

    @Override
    public void Insert(T var) {
        // TODO Auto-generated method stub
        if(head==null){
            Initial(var);
            return;
        }
        Node<T> temp=head;
        Node<T> node=new Node<>();
        createNode(node);
        node.data.saveData=var;
        while(temp.next!=head){
            temp=temp.next;
        }
        node.next=head;
        temp.next=node;
    }
    @Override
    public void Show(Cyclelist<T> list) {
        // TODO Auto-generated method stub
        Node<T> temp=list.head;
        while(temp.next!=head){
            System.out.print(temp.data.saveData);
            temp=temp.next;
        }
        System.out.println(temp.data.saveData);
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
