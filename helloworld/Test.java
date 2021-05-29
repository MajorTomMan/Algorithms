class Test{
    public static void main(String[] args) {
        Iusb electronics=new Mouse();  //新建接口变量
        electronics.Input();
        electronics.Output();
        electronics.Charge();
        electronics=new Keyboard();
        electronics.Input();
        electronics.Output();
        electronics.Charge();
    }
}


interface Iusb{
    public void Input();
    public void Output();
    public void Charge();
}


class Mouse implements Iusb{
    @Override
    public void Input() {
        // TODO Auto-generated method stub
        System.out.println("插入成功(鼠标)");
    }

    @Override
    public void Output() {
        // TODO Auto-generated method stub
        System.out.println("拔出成功(鼠标)");
    }
    public void Charge(){
        System.out.println("我在充电(鼠标)");
    }
}
class Keyboard implements Iusb{
    @Override
    public void Input() {
        // TODO Auto-generated method stub
        System.out.println("插入成功(键盘)");
    }

    @Override
    public void Output() {
        // TODO Auto-generated method stub
        System.out.println("拔出成功(键盘)");
    }
    public void Charge(){
        System.out.println("我在充电(键盘)");
    }
    
}