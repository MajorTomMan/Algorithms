
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
