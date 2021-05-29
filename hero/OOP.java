public class OOP{
    public static void main(String[] args) {
       // Snake xx=new SnakeHuge(); //xx为对象变量,本质为指针,初始化为null,当new Snake赋值给xx时,实质上是将堆中保存的内容和指针相联系,栈中保存的为地址,堆中为内存,new方法等于将堆中内存的地址赋值在指针中并保存在栈中
  //      person stones=new person();
        dragon balongma=new dragon();
        pig zhubajie=new pig();
     //   stones.CommandMosterl(zhubajie);
        IFly bird=new bird();
        flight airplane=new flight();
        IFly shalu=bird;
        shalu.Fly(); 
        Person stone=new Person();
        stone.Fly(bird);
    }
}




//私有的无法继承

class Snake{
    protected int level;
    protected  int age;
    public void Fire(){
        System.out.println("I'am Fire");
    }

}

// 带角有腿
class SnakeHuge extends Snake{
    Boolean HasFoot=true;
    Boolean HasLegs=true;
    String name;
    public void sayName(){
        System.out.println("我的名字"+name);
    }
    public int GetAge(){
        return age;
    }
    @Override //重写
    public void Fire(){
        System.out.println("Son's Fire");
    }
    public void Do(){
        super.Fire();
    }
}



class Monsterbase{
    public void Attack(){
        System.out.println("琢");
    }
}


class dragon extends Monsterbase{
    @Override
    public void Attack() {
        System.out.println("铁头功");
    }
}

class pig extends Monsterbase{
    @Override
    public void Attack() {
        System.out.println("铁头功");
    }
}

class cayman extends Monsterbase{
    @Override
    public void Attack() {
        System.out.println("铁头功");
    }
}
/*
class person{
    void CommandMosterl(Monsterbase xx){
        xx.Attack();
    }
}
*/

interface IFly{
    public void Fly();
}

class flight implements IFly{
    @Override
    public void Fly() {
        // TODO Auto-generated method stub
        System.out.println("我要飞");
    }

}

class bird implements IFly{
    @Override
    public void Fly() {
        // TODO Auto-generated method stub
        System.out.println("我要飞飞飞");
    }
}

class Person{
   public void Fly(IFly xx){
        xx.Fly();
   }
}