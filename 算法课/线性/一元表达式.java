/*
public class 一元表达式{
    public static void main(String[] args) {
        Statement statement=new Statement();
        Node node=new Node();
        node.setCoef(2);
        node.setExp(0);
        Node node_b=new Node();
        node_b.setCoef(-1);
        node_b.setExp(0);
        statement.Initial(node,node_b);
        Node n=new Node();
        n.setCoef(-1);
        n.setExp(1);
        statement.AddatTail(n);
        Node n1=new Node();
        n1.setCoef(1);
        n1.setExp(1);
        statement.AddatTail_B(n1);
        Node n2=new Node();
        n2.setCoef(1);
        n2.setExp(2);
        statement.AddatTail(n2);
        Node n3=new Node();
        n3.setCoef(-1);
        n3.setExp(2);
        statement.AddatTail_B(n3);
        statement.Show(statement);
        statement.Add();
        statement.Show(statement);
    }
}


class Node{
    private int coef; //系数
    private int exp; //指数
    Node next=null;
    public int getCoef() {
        return coef;
    }
    public void setCoef(int coef) {
        this.coef = coef;
    }
    public int getExp() {
        return exp;
    }
    public void setExp(int exp) {
        this.exp = exp;
    }
    public Node getNext() {
        return next;
    }
    public void setNext(Node next) {
        this.next = next;
    }
    @Override
    public String toString() {
        return "Node [coef=" + coef + ", exp=" + exp + ", next=" + next + "]";
    }
    
}


class Statement{
    Node Head;
    Node Head_B;
    public void Initial(Node node,Node node_b){
        Head=new Node();
        Head.next=node;
        Head_B=new Node();
        Head_B.next=node_b;
    }
    public void AddatTail(Node node){
        Node temp=Head.next;
        while(temp.next!=null){
            temp=temp.next;
        }
        temp.next=node;
    }
    public void Show(Statement statement){
        Node temp=statement.Head.next;
        while(temp!=null){
            System.out.println(temp.toString());
            temp=temp.next;
        }
    }
    public void AddatTail_B(Node node){
        Node temp=Head_B.next;
        while(temp.next!=null){
            temp=temp.next;
        }
        temp.next=node;
    }
    public void Add(){
        Node Front=Head;
        Node p=Front.getNext();  
        Node q=Head_B.getNext();
        while(Front!=null){
            if(p!=null){
                if(p.getExp()==q.getExp()){
                    if(p.getCoef()+q.getCoef()==0){
                        Front.setNext(p.getNext());
                        p=Front.getNext();
                        q=q.getNext();
                    }
                    else{
                        p.setCoef(p.getCoef()+q.getCoef());
                        Front=p;
                        p=p.getNext();
                        q=q.getNext();
                    }
                }
                else if(p.getExp()<q.getExp()){
                    Front=p;
                    p=p.getNext();
                }
                else if(p.getExp()>q.getExp()){
                    Node temp=new Node();
                    temp.setCoef(q.getCoef());
                    temp.setExp(q.getExp());
                    Front.setNext(temp);
                    temp.setNext(p);
                    Front=Front.getNext();
                    q=q.getNext();
                }
            }
            else{
                if(Front.getNext()==null){
                    Front.setNext(q.getNext());
                }
                else{
                    Front=Front.getNext();
                }
            }
        }
    }
}



*/