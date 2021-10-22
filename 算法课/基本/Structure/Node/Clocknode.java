package 基本.Structure.Node;

public class Clocknode{
    String Intruduce;
    Byte Used;
    public Clocknode(String intruduce,Byte used) {
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
