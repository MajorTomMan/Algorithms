package 查找.Structure;

public abstract class SymbolTable<Key,Value>{
   public abstract void put(Key key, Value val);//加入键值对
   public abstract Value get(Key key); //获取键Key对应的值
   protected abstract int size();
   public abstract Iterable<Key> keys();
   protected void delete(Key key){
      put(key,null);
   }
   protected boolean contains(Key key){
      return get(key)!=null;
   }
   protected boolean isEmpty(){
      return size()==0;
   }
}
