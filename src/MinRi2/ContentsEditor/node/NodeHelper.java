package MinRi2.ContentsEditor.node;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.ctype.*;
import mindustry.mod.*;

import java.lang.reflect.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class NodeHelper{
    private static Object root;
    private static ContentParser parser;
    private static ObjectMap<String, ContentType> nameToType;

    public static Object getRootObj(){
        if(root == null) root = Reflect.get(ContentPatcher.class, "root");
        return root;
    }

    public static ContentParser getParser(){
        if(parser == null) parser = Reflect.get(ContentPatcher.class, "parser");
        return parser;
    }

    public static OrderedMap<String, FieldMetadata> getFields(Class<?> type){
        return getParser().getJson().getFields(type);
    }

    public static ObjectMap<String, ContentType> getNameToType(){
        if(nameToType == null) nameToType = Reflect.get(ContentPatcher.class, "nameToType");
        return nameToType;
    }

    public static Class<?> getType(NodeData node){
        if(node.meta != null) return node.meta.field.getType();
        if(node.object == null) return null;
        Class<?> clazz = node.object.getClass();
        while(clazz.isAnonymousClass()) clazz = clazz.getSuperclass();
        return clazz;
    }

    public static String getKeyName(Object object){
        if(object instanceof MappableContent mc) return mc.name;
        if(object instanceof Enum<?> e) return e.name();
        if(object instanceof Class<?> clazz) clazz.getName();
        return object.toString();
    }

    public static boolean fieldEditable(Field field){
        if(field.isAnnotationPresent(NoPatch.class) || field.getDeclaringClass().isAnnotationPresent(NoPatch.class)){
            return false;
        }
        return true;
    }
}
