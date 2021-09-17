import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class thread_t implements Runnable{
    private Thread tr;
    private int num=0;
    private String name;
    public thread_t(int num,String name){
        this.num=num;
        this.name=name;
    }
    @Override
    public void run() {
        // TODO Auto-generated method stub
        System.out.println("1");
        System.out.println("2");
        System.out.println("3");
        System.out.println("hello,这是"+tr.getName()+",我已经进入运行状态了!");
        System.out.println("尝试创建文件...");
        tryto_read_file();
        System.out.println("1");
        System.out.println("2");
        System.out.println("3");
        System.out.println(tr.getName()+"执行完了run方法,是时候该合上自己的眼了,阿门");
        System.out.println("("+tr.getName()+"执行完run方法进入死亡状态)");
    }
    public void start(){
        if(tr==null){
            tr=new Thread(this, name);
        }
        System.out.println("hello,"+tr.getName()+"现在处于新建状态,现在马上就要进入就绪状态了!");
        System.out.println("1");
        System.out.println("2");
        System.out.println("3");
        tr.start();
        System.out.println(tr.getName()+"现在进入就绪状态了!");
    }
    private void tryto_read_file(){
        File file=new File("C:\\Users\\Administrator\\Desktop\\threaddata.java");
        try {
            System.out.println(tr.getName()+"创建或读取文件成功!");
            FileWriter fw=new FileWriter(file,true);
            fw.append("进程"+num+"写入中....\n");
            fw.append("进程"+num+"写入中....\n");
            fw.append("进程"+num+"写入中....\n");
            fw.append("写入完成!\n");
            fw.append("-----------\n");
            System.out.println(tr.getName()+"在休眠......");
            Thread.sleep(1000*30);
            System.out.println(tr.getName()+"结束休眠了!");
            fw.close();
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            System.out.println("oh no "+tr.getName()+"找不到可以创建文件的地址或者我们创建不了文件!");
            e.printStackTrace();
        }
        
    }
}