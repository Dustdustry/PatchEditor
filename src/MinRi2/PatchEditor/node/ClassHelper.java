package MinRi2.PatchEditor.node;

import arc.struct.*;

public class ClassHelper{
    public static Class<?> unoymousClass(Class<?> clazz){
        if(clazz == null) return null;
        while(clazz.isAnonymousClass()) clazz = clazz.getSuperclass();
        return clazz;
    }

    public static boolean isArray(Class<?> type){
        return type != null && type.isArray();
    }

    public static boolean isArrayLike(Class<?> type){
        return type != null && (type.isArray() || Seq.class.isAssignableFrom(type) || ObjectSet.class.isAssignableFrom(type));
    }

    public static boolean isMap(Class<?> type){
        return type != null && (ObjectMap.class.isAssignableFrom(type) || ObjectFloatMap.class.isAssignableFrom(type));
    }

    public static String getDisplayName(Class<?> clazz){
        return clazz.getSimpleName() + (isArray(clazz) ? "[]" : "");
    }
}
