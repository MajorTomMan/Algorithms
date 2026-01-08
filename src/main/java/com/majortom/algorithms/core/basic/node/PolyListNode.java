
package com.majortom.algorithms.core.basic.node;

public class PolyListNode {
    private int power; // 系数
    private int exp; // 指数
    private PolyListNode next; // 指向下一个节点的指针

    public PolyListNode(int power, int exp, PolyListNode next) {
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

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
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
