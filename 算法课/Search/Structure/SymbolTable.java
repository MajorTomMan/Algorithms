package Search.Structure;

public abstract class SymbolTable<Key,Value>{
   public abstract void put(Key key, Value val);//加入键值对
   public abstract Value get(Key key); //获取键Key对应的值
   public abstract int size();
   public abstract Iterable<Key> keys();
   public void delete(Key key){
      put(key,null);
   }
   public boolean contains(Key key){
      return get(key)!=null;
   }
   public boolean isEmpty(){
      return size()==0;
   }
}
