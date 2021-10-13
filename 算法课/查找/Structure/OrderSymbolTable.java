package 查找.Structure;

public abstract class OrderSymbolTable<Key,Value> extends SymbolTable<Key,Value>{
    public abstract Key min(); //最小的键
    public abstract Key max(); //最大的键
    public abstract Key floor(Key key); //小于等于key的最大值
    public abstract Key ceiling(Key key); //大于等于key的最小值
    public abstract int rank(Key key); //小于key的键的数量
    public abstract Key select(int k); //排名为K的键
    public abstract Iterable<Key> keys(Key lo,Key hi); //已排序的以lo到hi这个区间内的数据
    public abstract int size(); //表中所有键的数量
    public abstract void delete(Key key);
    @Override
    public Iterable<Key> keys() { //所有表中键的集合
        // TODO Auto-generated method stub
        return keys(min(),max());
    }
    public void deleteMin(){ //删除最小的键
        delete(min());
    }
    public void deleteMax(){ //删除最大的键
        delete(max());
    }
    public int size(Key lo,Key hi){ //lo到hi这个区间内的键的数量
        if(((Comparable) hi).compareTo(lo)<0){
            return 0;
        }
        else if(contains(hi)){
            return rank(hi)-rank(lo)+1;
        }
        else{
            return rank(hi)-rank(lo);
        }
    }
}
