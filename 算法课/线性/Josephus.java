
/*

public class Josephus {
    public static void main(String[] args) {
        int i=3;
        CycleList cyList=Init();
        while(true){
            if(cyList.getLength()!=0){
                System.out.println(cyList.Delete(i));
                cyList.Show(cyList);
            }
            else{
                System.out.println(cyList.getHead().getData().toString());
                System.out.println("只剩最后一人");
                break;
            }
        }
    }
    public static CycleList Init(){
        int i=0;
        CycleList cycleList=new CycleList();
        while(i!=6){
            LinkedList data=new LinkedList();
            Node node=new Node();
            node.setData(data);
            node.getData().setId(i);
            if(cycleList.IsEmpty()){
                cycleList.Initital(node);
            }
            else{
               System.out.println(cycleList.Insert(node));
            }
            i++;
        }
        cycleList.Show(cycleList);
        return cycleList;
    }
}





class LinkedList{
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "LinkedList [id=" + id + "]";
    }

}

class Node{ 
    private LinkedList Data;
    private Node next;
    public LinkedList getData() {
        return Data;
    }
    public void setData(LinkedList data) {
        Data = data;
    }
    public Node getNext() {
        return next;
    }
    public void setNext(Node next) {
        this.next = next;
    }
}


class CycleList{
    private Node head;
    private Node rear;
    private int length;
    public void Initital(Node node){
        head=node;
        rear=node;
        head.setNext(rear);
        rear.setNext(head);
    }
    public String Insert(Node node){
        if(IsEmpty()){
            return "链表为空";
        }
        Node temp=head;
        while(temp.getNext()!=head){
            temp=temp.getNext();
        }
        temp.setNext(node);
        node.setNext(rear);
        length++;
        return "该节点添加完成";
    }
    public String Delete(int Index){
        int i=0;
        Node temp=head;
        Node pre=new Node();
        if(IsEmpty()){
            return "链表为空";
        }
        while(i!=Index){
            pre=temp;
            temp=temp.getNext();
            i++;
        }
        head=temp.getNext();
        pre.setNext(head);
        length--;
        System.out.println("删除的节点数据为:"+temp.getData().toString());
        return "该节点已删除";
    }
    public void Show(CycleList CyList){
        Node temp=CyList.head;
        while(temp.getNext()!=head){
            System.out.println(temp.getData().toString());
            temp=temp.getNext();
        }
        if(length!=0){
            System.out.println(temp.getData().toString());
        }
    }
    public boolean IsEmpty(){
        if(head==null){
            return true;
        }
        return false;
    }
    public int getLength() {
        return length;
    }
    public void setLength(int length) {
        this.length = length;
    }
    public Node getHead() {
        return head;
    }
    public void setHead(Node head) {
        this.head = head;
    }
    public Node getRear() {
        return rear;
    }
    public void setRear(Node rear) {
        this.rear = rear;
    }
}


*/