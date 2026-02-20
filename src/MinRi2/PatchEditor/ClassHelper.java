package MinRi2.PatchEditor;

import arc.struct.*;

import java.lang.reflect.*;

public class ClassHelper{
    public static Class<?> unoymousClass(Class<?> clazz){
        if(clazz == null) return null;
        while(clazz.isAnonymousClass()) clazz = clazz.getSuperclass();
        return clazz;
    }

    public static boolean isAbstractClass(Class<?> clazz){
        return Modifier.isAbstract(clazz.getModifiers()) && !clazz.isInterface() && !clazz.isArray();
    }

    public static boolean isArray(Class<?> type){
        return type != null && type.isArray();
    }

    public static boolean isContainer(Class<?> type){
        return isArrayLike(type) || isMap(type);
    }

    public static boolean isArrayLike(Class<?> type){
        return type != null && (type.isArray() || Seq.class.isAssignableFrom(type) || ObjectSet.class.isAssignableFrom(type) || EnumSet.class.isAssignableFrom(type));
    }

    public static boolean isMap(Class<?> type){
        return type != null && (ObjectMap.class.isAssignableFrom(type) || ObjectFloatMap.class.isAssignableFrom(type));
    }

    public static String getDisplayName(Class<?> clazz){
        return clazz.getSimpleName() + (isArray(clazz) ? "[..]" : "");
    }
}
