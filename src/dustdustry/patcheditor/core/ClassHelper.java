package dustdustry.patcheditor.core;

import arc.struct.*;

import java.lang.reflect.*;

public class ClassHelper{

    public static Class<?> actualClass(Class<?> clazz){
        if(clazz == null) return null;
        if(isLambda(clazz)) return clazz.getInterfaces()[0];
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

    public static boolean isLambda(Class<?> clazz){
        return clazz.isSynthetic() && clazz.getName().contains("$$Lambda");
    }
}
