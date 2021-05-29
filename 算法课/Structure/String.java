package Structure;

public class Override_String implements IString {
    private char[] str;
    public Override_String(String s){
        str=s.toCharArray();
    }
    public int length(){
        return str.length;
    }
    public char charAt(int index){
        return str[index];
    }
    @Override
    public boolean equals(String str1){
        int i=0;
        int flag=1;
        if(str1.length()!=str.length){
            return false;
        }
        while(flag){
            if(str1.charAt(i)!=str[i]){
                flag=0;
            }
            if(i==str1.length()-1){
                break;
            }
            i++;
        }
        if(flag){
            return true;
        }else{
            return false;
        }
    }
    public T indexof(String str1){
        char[] finds=new char[str1.length()];
        int first_index=0;
        int[] temp=new int[str1.length()];

        if(str1.length()>str.length){
            return "子串长度超过父串";
        }
        int i=0,j=0,k=0;
        while(i+str1.length()>str.length){
            finds=subString(i,str1.length());
            while(j!=str1.length()){
                if(finds[j]!=str1.charAt(i)){
                    first_index=-1;
                }
                else{
                    first_index=j;
                    temp[k]=first_index;
                    k++;
                }
                j++;
            }
            j=0;
            i=str1.length()+i;
        }
        int result=str.length-i;
        j=0;
        while(j!=result){
            if(finds[j]!=str1.charAt(i)){
                first_index=-1;
            }
            else{
                first_index=j;
                temp[k]=first_index;
                k++;
            }
            j++;
        }
        return temp;
    }
    public String replace(char oldchar,char newchar){
        int i=0;
        while(i!=str.length){
            if(str[i]==oldchar){
                str[i]=newchar;
            }
        }
        return "替换完成";
    }
    public char[] subString(int startindex,int endindex){
        char[] temp=new char[endindex-startindex];
        int i=0;
        while(startindex!=endindex){
            temp[i]=str[startindex];
            startindex++;
        }
        return temp;
    }
    public String toString(){
        for (char c : str) {
            System.out.print(c);
        }
    }
}
