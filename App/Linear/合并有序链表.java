import Basic.Structure.Linkedlist;
import Basic.Structure.Node.Node;

public class 合并有序链表 {
    public static void main(String[] args) {
        Linkedlist<Integer> list_1=new Linkedlist<>(1);
        Linkedlist<Integer> list_2=new Linkedlist<>(3);
        list_1.InsertTail(2);
        list_1.InsertTail(6);
        list_2.InsertTail(4);
        list_2.InsertTail(5);
        Linkedlist<Integer> list_3=new Linkedlist<>(mergeTwoLists(list_1.getHead(),list_2.getHead()));
        list_3.Show(list_3.getHead());
    }
    public static Node<Integer> mergeTwoLists(Node<Integer> list1, Node<Integer> list2) {
        if(list1==null){
            return list2;
        }
        else if(list2==null){
            return list1;
        }
        else if (list1.data.saveData<list2.data.saveData) {
            list1.next = mergeTwoLists(list1.next,list2);
            return list1;
        } else {
            list2.next = mergeTwoLists(list1,list2.next);
            return list2;
        }
    }
}
