/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 18:50:03
 * @FilePath: /alg/Algorithms/basic/structure/node/PolyListNode.java
 */
package basic.structure.node;

public class PolyListNode {
    private double power; // 系数
    private int exp; // 指数
    private PolyListNode next; // 指向下一个节点的指针

    public PolyListNode(double power, int exp, PolyListNode next) {
        this.power = power;
        this.exp = exp;
        this.next = next;
    }

    public PolyListNode() {
    }

    @Override
    public String toString() {
        return """
                {
                    power:%s
                    exp:%s
                    next-> %s
                }\n
                """.formatted(power, exp, next);
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public PolyListNode getNext() {
        return next;
    }

    public void setNext(PolyListNode next) {
        this.next = next;
    }
}
