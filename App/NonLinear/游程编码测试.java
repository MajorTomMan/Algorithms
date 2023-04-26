package NonLinear;

import java.io.IOException;

import character.datacompression.RunLength;


public class 游程编码测试 {
    public static void main(String[] args) throws IOException {
        if(args[0].equals("-")){
            RunLength.compress();
        } 
        else if (args[0].equals("+")){
            RunLength.expand();
        } 
        else{
            throw new IllegalArgumentException("Illegal command line argument");
        }
    }
}
