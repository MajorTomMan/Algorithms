package 基本.Structure.Interface;

import 基本.Structure.Node.Clocknode;

public interface ILoopqueue {
    Clocknode dequeue();
    void enqueue(Clocknode var);
    boolean isEmpty();
    Clocknode getFront();
    int getSize();
}
