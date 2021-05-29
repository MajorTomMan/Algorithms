class Between0and1{
    public static void main(String[] args) {
        double x=0.0,y=0.0;
        x=Double.parseDouble(args[0]);
        y=Double.parseDouble(args[1]);
        if(x>0 && x<1 && y>0 && y<1){
            System.out.println(true);
        }
        else{
            System.out.println(false);
        }
    }
}