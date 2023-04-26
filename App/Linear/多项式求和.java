import basic.structure.PolyList;

/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 18:49:09
 * @FilePath: /alg/App/Linear/多项式求和.java
 */


 
public class 多项式求和 {
    public static void main(String[] args) {
        PolyList list1=new PolyList();
        PolyList list2=new PolyList();
        list1.insert(2, 3);
        list1.insert(3, 2);
        list1.insert(4, 5);
        list1.insert(5, 4);
        list1.show();
        System.out.println("---------------");
        list1.delete(3, 2);
        list1.delete(2, 3);
        list1.show();
    }

    /*
     * 1.如果L1所指向节点的exp大于L2所指向节点的exp，也就是L1 -> exp > L2 ->
     * exp，则不需要相加，只需要把L1所指向节点的coef和exp拷贝到新的节点中，然后把新节点插入到求和的链表中。同时，还要让L1指针向后移动一个位置。
     * 2.如果L2所指向节点的exp大于L1所指向节点的exp，也就是L2 -> exp > L1 ->
     * exp，则不需要相加，只需要把L2所指向节点的coef和exp拷贝到新的节点中，然后把新节点插入到求和的链表中。同时，还要让L2指针向后移动一个位置。
     * 
     * 3.如果L2所指向节点的exp等于L1所指向节点的exp，也就是L2 -> exp == L1 ->
     * exp，我们就需要对两个这节点的coef进行相加，把相加的结果存放到新节点的coef中，同时也要把当前的exp存放到新节点中。
     * 再来对相加后的coef进行判断，如果相加后的coef ==0，
     * 那么就不需要把它插入到求和的链表中，同时把新节点通过关键字delete删除。如果相加后的coef !=0
     * 我们就把新节点插入到求和的链表中。最后，别忘了让L1和L2向后移动一个位置。
     * 
     * 4.当退出循环后，说明其中一个多项式已经为空了，这个时候我们只需要找到那个还没有空的多项式，然后把该多项式剩余的那部分节点都接到求和链表的后面，
     * 就完成了两个多项式求和这个过程了。当然，如果退出时两个多项式都为空了，我们同样可以把其中的一个多项式接到求和链表的后面，只不过这个时候接的是NULL。
     */
}
