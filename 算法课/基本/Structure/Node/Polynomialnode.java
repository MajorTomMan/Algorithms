package 基本.Structure.Node;

public class Polynomialnode{
    private double power;
    private int exp;
    private Polynomialnode next;
    public Polynomialnode(double power, int exp, Polynomialnode next) {
        this.power = power;
        this.exp = exp;
        this.next = next;
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
    public Polynomialnode getNext() {
        return next;
    }
    public void setNext(Polynomialnode next) {
        this.next = next;
    }
    @Override
    public String toString() {
        return "Polynomialnode [exp=" + exp + ", next=" + next + ", power=" + power + "]";
    }
}
