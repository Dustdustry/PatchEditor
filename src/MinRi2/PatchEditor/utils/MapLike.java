package MinRi2.PatchEditor.utils;

import arc.struct.*;

public class MapLike{
    public static boolean contains(Object object, Object key){
        return object instanceof ObjectMap map1 && map1.containsKey(key)
        || object instanceof ObjectFloatMap map2 && map2.containsKey(key);
    }
}
