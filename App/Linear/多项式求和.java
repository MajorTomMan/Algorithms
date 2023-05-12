import basic.structure.PolyList;
import basic.structure.node.PolyListNode;

/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-27 11:56:30
 * @FilePath: /alg/App/Linear/多项式求和.java
 */

public class 多项式求和 {
    /*
     * A:7+3x+9x^8+5x^17
     * B:8x+22x^7-9x^8
     * sum:7+11x+22x^7+5x^17
     */
    public static void main(String[] args){
        PolyList list1 = new PolyList();
        PolyList list2 = new PolyList();
        list1.insert(7, 0);
        list1.insert(3, 1);
        list1.insert(9, 8);
        list1.insert(5, 17);
        list2.insert(8, 1);
        list2.insert(22, 7);
        list2.insert(-9, 8);
        PolyList sumList = add(list1, list2);
        sumList.show();
    }

    /*
     * 关键是把握指针何时需要往前走
     */
    public static PolyList add(PolyList L1, PolyList L2) {
        /* 如果链表为空 */
        if (L1.isEmpty() || L2.isEmpty()) {
            return null;
        }
        /* 结果链表 */
        PolyList sumList = new PolyList();
        /* 指向两个链表的指针 */
        PolyListNode node1 = L1.getHead(), node2 = L2.getHead();
        for (; node1 != null && node2 != null;) {
            /*
             * 如果A链表的节点指数要小于B链表的节点指数
             * 那么结果链表先将A链表的节点加入其中,
             * 再将指向A的指针指向下一个节点
             * 指向B的指针不动
             * 如果B链表的节点指数小于A链表
             * 那么结果链表先将B链表的节点加入其中,
             * 再将指向B的指针指向下一个节点
             * 指向A的指针不动
             * 如果两者相等,则分情况讨论
             * 若两者系数相加等于0,等价于两个相反数相加结果必然为0,结果链表不需要加入已经相消的节点
             * 若两者系数相加不等于0,将两者的系数相加后加入结果链表
             * 两种情况在处理完以后都需要将指针往后移,因为此时该节点已经处理完了
             * 需要处理下一个节点的情况
             */
            if (node1.getExp() < node2.getExp()) {
                sumList.insert(node1.getPower(), node1.getExp());
                node1 = node1.getNext();
            } else if (node1.getExp() > node2.getExp()) {
                sumList.insert(node2.getPower(), node2.getExp());
                node2 = node2.getNext();
            }
            if (node1.getExp() == node2.getExp()) {
                int sum = node1.getPower() + node2.getPower();
                if (sum != 0) {
                    sumList.insert(sum, node1.getExp());
                }
                node1 = node1.getNext();
                node2 = node2.getNext();
            }
        }
        /*
         * 因为多项式的项数是不确定的,
         * 所以需要一个保险措施，
         * 检查有无剩余的节点还没处理
         * 将剩下的节点加入结果链表
         * 无论是哪个链表
         */
        if (node1 == null) {
            while (node2 != null) {
                sumList.insert(node2.getPower(), node2.getExp());
                node2 = node2.getNext();
            }
        } else {
            while (node1 != null) {
                sumList.insert(node1.getPower(), node1.getExp());
                node1 = node1.getNext();
            }
        }
        return sumList;
    }
}
