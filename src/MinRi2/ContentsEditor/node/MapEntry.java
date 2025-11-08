package MinRi2.ContentsEditor.node;

import arc.struct.*;

public class MapEntry<K, V>{
    public K key;
    public V value;

    public MapEntry(K key, V value){
        this.key = key;
        this.value = value;
    }

    public MapEntry(ObjectMap.Entry<K, V> entry){
        this(entry.key, entry.value);
    }

    @Override
    public String toString(){
        return "MapEntry{" +
        "key=" + key +
        ", value=" + value +
        '}';
    }
}
