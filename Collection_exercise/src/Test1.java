

public class Test1 {
  //THis is My first Test Code 
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Hello World");
		Test1_1 te=new Test1_1();
		Object[] ut=te.Method();
		for(Object ot:ut){
		System.out.println(ot);
		}
	}

}


class Test1_1{
     Object[] Method() {
	 Object[] output={'h','e','l','l','o','w','o','r','l','o'};
	 return output;
}	
     @Override
    public String toString() {
    	// TODO Auto-generated method stub
    	return super.toString();
    }
}