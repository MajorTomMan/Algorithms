package 基本.Structure;

public class OverrideString{
	private char[]  datas;
	private int length;
	public OverrideString(char[] datas) {
	
	    this.datas = datas;
		this.length=datas.length;
	}
	public OverrideString(String s) {
		super();
		this.datas = s.toCharArray();
		length=datas.length;
	}
	//1.返回串的长度
	public int Length(){
		return length;
	}
	//2.取出指定索引位置的字符
	public char charAt(int index){
		return datas[index];
	}
	//3.判断当前串是否与指定串相等
	public boolean equals(OverrideString str){
		if(length!=str.length){
			return false;
		}
		for(int i=0;i<length;i++){
			if(datas[i]!=str.datas[i]){
				return false;
			}
		}
		return true;
	}
	//4.检索子串出现在当前串的位置，如果未检索到，返回-1
	//1234567890
	//123
	public int indexOf(OverrideString str){
		int index=-1;
		for(int i=0;i<=length-str.length;i++){
			OverrideString substr = substring(i, i+str.length);
			if(substr.equals(str)){
				return i;
			}
		}
		return -1;
	}
	
	//5.替换当前串中的oldChar为newChar
	//I am a student,I love Java
	//a   A
	public OverrideString replace(char oldChar,char newChar){
		char[] cs = new char[length];
		for(int i=0;i<length;i++){
			if(datas[i]==oldChar){
				cs[i]=newChar;
			}else{
				cs[i]=datas[i];
			}
		}
		return new OverrideString(cs);
	}
	//5.截取子串，从当前串的startIndex位置开始，到endIndex位置前一个字符 
	public OverrideString substring(int startIndex, int endIndex){
		char[] cs = new char[endIndex-startIndex];
		if(startIndex<0  || endIndex<=startIndex  || endIndex>length){
			throw new IndexOutOfBoundsException("截取子串越界~~");
		}
		for(int i=startIndex;i<endIndex;i++){
			cs[i-startIndex]=datas[i];
		}
		OverrideString str=new OverrideString(cs);
		return str;
		//return new StatStr(cs);
	}

	@Override
	public String toString() {
		return new String(datas);
	}
}
