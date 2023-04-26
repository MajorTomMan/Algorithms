/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 18:46:28
 * @FilePath: /alg/Algorithms/basic/structure/iface/IPolyList.java
 */
package basic.structure.iface;

import basic.structure.node.PolyListNode;

public abstract class IPolyList{
    protected PolyListNode head;
    public abstract void insert(double power,int exp);
    public abstract void delete(double power,int exp);
    protected void clear(){
        head=clear(head);
    }
    private PolyListNode clear(PolyListNode node){
        if(node==null){
            return null;
        }
        PolyListNode temp=clear(node.getNext());
        return temp;
    }
    protected void show(){
        show(head);
    }
    private PolyListNode show(PolyListNode node){
        if(node!=null){
            System.out.println(node);
        }
        if(node==null){
            return node;
        }
        return show(node.getNext());
    }
}
