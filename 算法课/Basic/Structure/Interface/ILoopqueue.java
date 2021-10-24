package Basic.Structure.Interface;

import Basic.Structure.Node.Clocknode;

public interface ILoopqueue {
    Clocknode dequeue();
    void enqueue(Clocknode var);
    boolean isEmpty();
    Clocknode getFront();
    int getSize();
}
