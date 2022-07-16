package Basic.Structure;
import Basic.Structure.Interface.IPolynomial;
import Basic.Structure.Node.Polynomialnode;

public class Polynomial implements IPolynomial{
    private Polynomialnode head;
    @Override
    public void Insert(double power, int exp) {
        // TODO Auto-generated method stub
        Polynomialnode temp=head;
        Polynomialnode node=new Polynomialnode(power, exp,null);
        if(head==null){
            head=new Polynomialnode(power, exp);
            return;
        }
        while(temp.getNext()!=null){
            temp=temp.getNext();
        }
        temp.setNext(node);
    }
}
