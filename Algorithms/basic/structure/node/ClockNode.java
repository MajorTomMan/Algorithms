/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:58:00
 * @FilePath: /alg/Algorithms/basic/structure/node/ClockNode.java
 */
package basic.structure.node;

public class ClockNode{
    String Intruduce;
    Byte Used;
    public ClockNode(String intruduce,Byte used) {
        Intruduce = intruduce;
        Used = used;
    }
    public String getIntruduce() {
        return Intruduce;
    }
    public void setIntruduce(String intruduce) {
        Intruduce = intruduce;
    }
    public Byte getUsed() {
        return Used;
    }
    public void setUsed(Byte used) {
        Used = used;
    }
    
}
