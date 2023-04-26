/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 18:44:55
 * @FilePath: /alg/Algorithms/basic/structure/PolyList.java
 */
package basic.structure;

import basic.structure.iface.IPolyList;
import basic.structure.node.PolyListNode;

/*
 * 一元多项式可以通过记录一元多项式各项的指数和系数值，
 * 使用按指数升序排列的线性表进行存储。
 * 带头结点升序排列单链表存储一元多项式，
 * 并实现两一元多项式的加法和乘法运算。输入n（1<=n<=100）,
 * 按指数升序顺序输入第一个一元多项式中n项指数和系数，输入m1<=m<=100）
 * ，按指数升序顺序输入第二个一元多项式中m项指数和系数。
 * 然后按照特定格式输入出两一元多项式的和与乘积结果。
 */

public class PolyList extends IPolyList {
    @Override
    public void insert(double power, int exp) {
        // TODO Auto-generated method stub
        if (head == null) {
            head = new PolyListNode(power, exp, null);
            return;
        }
        head = insert(power, exp, head);
    }

    private PolyListNode insert(double power, int exp, PolyListNode node) {
        // TODO Auto-generated method stub
        if (node == null) {
            node = new PolyListNode(power, exp, null);
            return node;
        }
        PolyListNode temp= insert(power, exp, node.getNext());
        node.setNext(temp);
        return node;
    }

    @Override
    public void delete(double power, int exp) {
        // TODO Auto-generated method stub
        if (head == null) {
            return;
        }
        if (head.getPower() == power && head.getExp() == exp) {
            if(head.getNext()==null){
                head=null;
            }
            else{
                head=head.getNext();
            }
            return;
        }
        for (PolyListNode pre = head,current = head.getNext(); current != null; pre = current, current = current.getNext()) {
            if (current.getPower() == power && current.getExp() == exp) {
                pre.setNext(current.getNext());
                current = null;
                return;
            }
        }
    }

    @Override
    public void show() {
        // TODO Auto-generated method stub
        super.show();
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        super.clear();
    }
}
