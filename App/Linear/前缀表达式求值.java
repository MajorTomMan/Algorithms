package linear;
/*
import java.util.Scanner;

import Structure.Stack;

public class 前缀表达式求值 {
    public static void main(String[] args) {
        Stack<String> ops=new Stack<String>(); //S1
        Stack<String> vals=new Stack<String>(); //S2
        System.out.print("请输入要计算的表达式:");
        Scanner scanner=new Scanner(System.in);
        String context=scanner.nextLine();
        scanner.close();
        context=transform(ops, vals, context);
        calculate(context);
    }
    public static String transform(Stack<String> ops,Stack<String> vals,String context){
        int i=0;
         i=context.length()-1;
        while(i!=-1){
            String ch=Character.toString(context.charAt(i));
            if(ch.equals("*")||ch.equals("/")||ch.equals("+")||ch.equals("-")||ch.equals("(")||ch.equals(")")){
                if(ops.getTop()==null||ch.equals(")")){
                    ops.push(ch);
                }
                else if(ch.equals("*")||ch.equals("/")&&ops.getTop().data.saveData.equals("+")||ops.getTop().data.saveData.equals("-")){
                    ops.push(ch);
                }
                else if(ch.equals("+")||ch.equals("-")&&ops.getTop().data.saveData.equals(")")){
                    if(ops.getTop().data.saveData.equals("*")||ops.getTop().data.saveData.equals("/")){
                        vals.push(ops.pop());
                        continue;
                    }
                    else{
                        ops.push(ch);
                    }
                }
                else if(ch.equals("+")||ch.equals("-")&&ops.getTop().data.saveData.equals("*")||ops.getTop().data.saveData.equals("/")){
                    vals.push(ops.pop());
                    continue;
                }
                else if(ch.equals("(")){
                    while(true){
                        String temp=ops.pop();
                        if(!temp.equals(")")){
                            vals.push(temp);
                        }
                        else{
                            break;
                        }
                    }
                }
            }
            else{
                vals.push(ch);
            }
            i--;
        }
        while(!ops.isEmpty()){
            vals.push(ops.pop());
        }
        char[] tempstr=new char[vals.getSize()];
        i++;
        while(!vals.isEmpty()){
            String str=vals.pop();
            tempstr[i]=str.charAt(0);
            System.out.print(str+",");
            i++;
        }
        i=tempstr.length-1;
        String s="";
        while(i!=-1){
           s=Character.toString(tempstr[i])+s;
           i--;
        }
        return s;
    }
    public static void calculate(String context){
        Double result;
        int i=context.length()-1;
        Stack<Double> stack=new Stack<Double>();
        while(i!=-1){
            String ch=Character.toString(context.charAt(i));
            if(ch.equals("*")||ch.equals("/")||ch.equals("+")||ch.equals("-")){
                switch(ch){
                    case "+": result=stack.pop()+stack.pop();stack.push(result);break;
                    case "-": result=stack.pop()-stack.pop();stack.push(result);break;
                    case "*": result=stack.pop()*stack.pop();stack.push(result);break;
                    case "/": result=stack.pop()/stack.pop();stack.push(result);break;
                }
            }
            else{
                stack.push(Double.parseDouble(ch));
            }
            i--;
        }
        System.out.println("运算结果是:"+stack.pop());
    }
}


*/