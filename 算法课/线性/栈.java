
/* 
public class 栈 {
    public static void main(String[] args) {
        int i=0;
        Stack stack=new Stack();
        while(i!=6){
            Node node=new Node();
            node.id=i;
            stack.Add(node);
            i++;
        }
        stack.Show(stack);
        i=0;
        while(i!=6){
            stack.Remove(stack);
            i++;
        }
        stack.Show(stack);
    }
}


class Node{
    public int id;
    Node next;
    @Override
    public String toString() {
        return "Node [id=" + id + ", next=" + next + "]";
    }
}

class Stack{
    Node stack;
    int length;
    public boolean isSpace(){
        if(stack!=null){
            return false;
        }
        return true;
    }
    public void Add(Node node){
        Node temp=stack;
        if(isSpace()){
            stack=node;
        }
        else{
            while(temp.next!=null){
                temp=temp.next;
            }
            temp.next=node;
        }
        length++;
    }
    public void Show(Stack Temp){
        Node temp=Temp.stack;
        while(temp.next!=null){
            System.out.println(temp.toString());
            temp=temp.next;
        }
    }
    public void Remove(Stack stack){
        Node temp=stack.stack;
        Node pre=new Node();
        while(temp.next!=null){
            pre=temp;
            temp=temp.next;
        }
        System.out.println("要删除的元素是:"+temp.toString());
        pre.next=null;
        length--;
    }
}

*/