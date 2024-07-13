package linear;


import basic.structure.LinkedList;

public class 链表 {
    public static void main(String[] args) {
        LinkedList<Integer> list = new LinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(6);
        list.add(8);
        list.add(10);
        list.add(1);
        list.add(2);
        list.sort();
        list.foreach((t) -> {
            System.out.println(t);
        });
        list.remove(1);
        list.replace(6, 1);
    }
}