package linear;

import basic.structure.TwoSideLink;
import basic.structure.node.TwoSideNode;

public class 双向链表 {
    public static void main(String[] args) {
        TwoSideLink<Integer> list = new TwoSideLink<>();
        System.out.println("原始链表：");
        showLink(list);
        for (int i = 0; i < 10; i++) {
            if(!list.add(i)){
                System.out.println("添加失败");
            }
        }
        System.out.println("向链表添加元素后：");
        showLink(list);
        System.out.println();
        System.out.println("向指定索引位置插入元素");
        if(!list.add(3,9)){
            System.out.println("添加失败");
        }
        showLink(list);
        System.out.println();
        System.out.println("删除头元素和尾元素");
        list.remove(0);
        list.remove(list.getSize());
        showLink(list);
        System.out.println();
        System.out.println("替换指定索引位置的元素");
        list.replace(2, 5);
        showLink(list);
        System.out.println();
        System.out.println("查询指定元素");
        System.out.println("查询元素6:" + list.find(6));
        System.out.println("返回指定索引位置的元素");
        System.out.println("查询索引位置2的元素:" + list.get(2));
        System.out.println();
        System.out.println("Pres List:");
        showPrevious(list);
        System.out.println();
        System.out.println("for each:");
        for(Integer data:list){
            System.out.print(" "+data);
        }
        System.out.println(list);
    }
    public static void showLink(TwoSideLink<?> list) {
        showLink(list.getHead());
    }

    private static TwoSideNode<?> showLink(TwoSideNode<?> node) {
        if (node == null) {
            return node;
        }
        System.out.print(" " + node.data);
        showLink(node.next);
        return node;
    }
    private static void showPrevious(TwoSideLink<?> list){
        TwoSideNode<?> temp=list.getLast();
        showPrevious(temp);
    }
    private static TwoSideNode<?> showPrevious(TwoSideNode<?> node){
        if(node==null){
            return node;
        }
        System.out.print(" "+node.data);
        showPrevious(node.pre);
        return node;
    }

}
