public class person {
    public static void main(String[] args) {
        thread_t tr_1=new thread_t(1,"线程1");
        thread_t tr_2=new thread_t(2,"线程2");
        tr_1.start(); //线程从新建状态调用start转入就绪状态,在cpu空闲时,程序调用run()来转入运行状态
        tr_2.start(); //在使用run()方法时,若调用wait()或synchronized同步锁抑或是使用sleep()等待或是join()方法获取io设备响应时
                      //线程就会进入阻塞状态,使cpu空闲出来,便于执行下一个线程,当阻塞状态解除(阻塞方法条件满足),线程重新进入就绪状态,等待下一步执行
    }
}
