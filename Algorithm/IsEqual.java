class IsEqual{
    public static void main(String[] args) {
        int[] num=new int[3];
        String temp="";
        for(int i=0;i<args.length;i++){
            temp=args[i];
            num[i]=Integer.parseInt(temp);
        }
        if(num[0]==num[1]&&num[1]==num[2]){
            System.out.println("Equal");
        }
        else{
            System.out.println("Not Equal");
        }
    }
}