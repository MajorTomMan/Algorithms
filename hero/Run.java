import java.util.Scanner;

public class Run {
    public static void main(String[] args) {
        int attack_number;
        Hero sunshangxiang=new Hero();
        Enemy BigDragon=new Enemy();
        Equipment_menu Emenu=new Equipment_menu();
        Action Action=new Action();
        attack_number=Action.action(sunshangxiang, BigDragon, Emenu);
        System.out.println("孙尚香攻击了"+attack_number+"次大龙");
    }
}



class Hero{
    int isDead;
    public int Attack(Enemy enemy,int attack_value){
        //找到攻击对象
        isDead=enemy.UnderAttack(attack_value);
        return isDead;
    }
}

class Enemy{
    int hp=100;
    int defense_value=10;
    public int UnderAttack(int attack_value){
        int isDead=0;
        if(attack_value>defense_value){
            hp-=attack_value-defense_value;
        }
        else{
            System.out.println("未击破对方护甲");
            isDead=-1;
        }
        if(hp<=0){
            isDead=1;
            Dead();
        }
        return isDead;
    }
    private void Dead(){
        System.out.println("大龙死了");
    }
}

class Equipment_menu{
    public int menu(){
        int attack_value=0;
        System.out.println("装备菜单:");
        System.out.println("1:M4步枪\t2:AR15步枪\t3:M16步枪");
        System.out.printf("请输入要购买的装备数字:");
        Scanner Getinput=new Scanner(System.in);
        int input=Getinput.nextInt();
        attack_value=choose(input);
        return attack_value;
    }
    private int choose(int input) {
        int attack_value=0;
        switch(input){
            case 1:attack_value=15;System.out.println("已购买"+"M4步枪"+" 攻击力为"+attack_value);break;
            case 2:attack_value=10;System.out.println("已购买"+"AR15步枪"+" 攻击力为"+attack_value);break;
            case 3:attack_value=20;System.out.println("已购买"+"M16步枪"+" 攻击力为"+attack_value);break;
            default:System.out.println("请输入正确的装备数字!");menu();
        }
        return attack_value;
    }
}

class Action{
    public int action(Hero HeroName,Enemy EnemyName,Equipment_menu Emenu) {
        boolean flag=true;
        int attack_value=0;
        int attack_number=0;
        int isDead=0;
        attack_value=Emenu.menu();
        while(flag){
            isDead=HeroName.Attack(EnemyName, attack_value);
            if(isDead==0){
                attack_number++;
            }
            else if(isDead==-1){
                System.out.println("您的武器未能击破对方护甲,请重新购买武器");
                attack_value=Emenu.menu();
            }
            else{
                flag=false;
            }
        }
        return attack_number;
    }
}
