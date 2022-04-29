package Basic.Structure;
import Basic.Structure.Interface.IPolynomial;
import Basic.Structure.Node.Polynomialnode;

public class Polynomial implements IPolynomial{
    private Polynomialnode head;
    @Override
    public void Initial(double power, int exp) {
        // TODO Auto-generated method stub
        head=new Polynomialnode(power, exp,null);
    }

    @Override
    public void Insert(double power, int exp) {
        // TODO Auto-generated method stub
        Polynomialnode temp=head;
        Polynomialnode node=new Polynomialnode(power, exp,null);
        if(head==null){
            Initial(power, exp);
        }
        while(temp.getNext()!=null){
            temp=temp.getNext();
        }
        temp.setNext(node);
    }

    @Override
    public void Show() {
        // TODO Auto-generated method stub
        Show(head);
    }
    public void Show(Polynomialnode node) {
        // TODO Auto-generated method stub
        if(node==null){
            return;
        }
        Show(node.getNext());
        System.out.println(node.getExp()+" "+node.getPower());
    }
}
