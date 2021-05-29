

/* 
public class 中缀表达式求值{
    public static void main(String[] args) {
        int i=0;
        Double result=0.0;
        Double temp=0.0;
        String statement="(1+((2+3)+(4+5)))";
        char[] state=statement.toCharArray();
        Character cr=' ';
        OpsStack<Character> ops=new OpsStack<Character>();
        OpsStack<Double> vals=new OpsStack<Double>();
        while(i!=state.length){
            if(state[i]=='('){
                i++;
                continue;
            }
            else if(state[i]==')'){
                cr=(Character)ops.pop();
                if(cr=='+'){
                    result=(Double)vals.pop();
                    temp=(Double)vals.pop();
                    result+=temp;
                    vals.push(result);
                }
                else if(cr=='-'){
                    result=(Double)vals.pop();
                    temp=(Double)vals.pop();
                    result-=temp;
                    vals.push(result);
                }
                else if(cr=='*'){
                    result=(Double)vals.pop();
                    temp=(Double)vals.pop();
                    result*=temp;
                    vals.push(result);
                }
                else if(cr=='/'){
                    result=(Double)vals.pop();
                    temp=(Double)vals.pop();
                    result/=temp;
                    vals.push(result);
                }
                i++;
                continue;
            }
            if(state[i]=='+'||state[i]=='-'||state[i]=='*'||state[i]=='/'){
                ops.push((char)state[i]);
            }
            else{
                vals.push(Double.parseDouble(Character.toString(state[i])));
            }
            i++;
        }
        System.out.println(statement+" 这句表达式计算结果是:"+vals.top.data.saveData);
    }
}


class OpsStack<T> implements IStack<T>{
    Node<T> top;
    int size;
    @Override
    public T pop() {
        // TODO Auto-generated method stub
        T data;
        data=top.data.saveData;
        top=top.next;
        size--;
        return data;
    }
    @Override
    public void push(T var) {
        Node<T> node=new Node<T>();
        if(isEmpty()){
            Inital(var);
            return;
        }
        createNode(node);
        node.data.saveData=var;
        node.next=top;
        top=node;
        size++;
    }
    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        if(top==null){
            return true;
        }
        else{ 
            return false;
        }
    }
    @Override
    public void Inital(T var) {
        // TODO Auto-generated method stub
        Node<T> node=new Node<T>();
        createNode(node);
        node.data.saveData=var;
        top=node;
        size++;
    }
    public int getSize(){
        return size;
    }
    private void createNode(Node<T> node){
        node.data=new Data<T>();
    }
}



*/