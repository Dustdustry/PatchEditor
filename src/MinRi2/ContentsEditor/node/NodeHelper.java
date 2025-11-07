package MinRi2.ContentsEditor.node;

import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.mod.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class NodeHelper{
    private static Object root;
    private static ContentParser parser;
    private static ObjectMap<String, ContentType> nameToType;

    private static final Seq<Class<?>> resolveBlacklist = Seq.with(
        Prov.class, Class.class, Texture.class, TextureRegion.class, Fi.class
    );

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
        if(node.getObject() == null) return null;
        Class<?> clazz = node.getObject().getClass();
        while(clazz.isAnonymousClass()) clazz = clazz.getSuperclass();
        return clazz;
    }

    public static String getKeyName(Object object){
        if(object instanceof MappableContent mc) return mc.name;
        if(object instanceof Enum<?> e) return e.name();
        if(object instanceof Class<?> clazz) return clazz.getName();
        return object.toString();
    }

    public static boolean typeBlack(Class<?> clazz){
        return resolveBlacklist.contains(black -> black.isAssignableFrom(clazz));
    }

    public static void resolveFrom(NodeData node, Object object){
        if(object == null) return; // ignore null?

        if(object == NodeHelper.getRootObj()){
            node.addChild("name", "root");
            var map = NodeHelper.getNameToType();
            for(ContentType ctype : ContentType.all){
                if(map.containsValue(ctype, true)){
                    node.addChild(ctype.toString().toLowerCase(Locale.ROOT), ctype);
                }
            }
        }else if(object instanceof Object[] arr){
            int i = 0;
            for(Object o : arr){
                String name = "" + i++;
                node.addChild(name, o);
            }
            node.addChild(ModifierSign.PLUS.sign, null, node.meta); // extend field meta
        }else if(object instanceof Seq<?> seq){
            for(int i = 0; i < seq.size; i++){
                Object o = seq.get(i);
                node.addChild("" + i, o);
            }
            node.addChild(ModifierSign.PLUS.sign, null, node.meta);
        }else if(object instanceof ObjectSet<?> set){
            int i = 0;
            for(Object o : set){
                node.addChild("" + i++, o);
            }
            node.addChild(ModifierSign.PLUS.sign, null);
        }else if(object instanceof ObjectMap<?, ?> map){
            for(var entry : map){
                String name = NodeHelper.getKeyName(entry.key);
                NodeData child = node.addChild(name, entry.value);
                child.addChild(ModifierSign.REMOVE.sign, null, node.meta);
            }
            // unaccessible
            if(!(node.getObject() instanceof Content)){
                node.addChild(ModifierSign.PLUS.sign, null);
            }
        }else if(object instanceof ContentType ctype){
            OrderedMap<String, Content> map = new OrderedMap<>(); // in order
            for(Content content : Vars.content.getBy(ctype)){
                map.put(NodeHelper.getKeyName(content), content);
            }
            resolveFrom(node, map);
        }else{
            for(var entry : NodeHelper.getFields(object.getClass())){
                String name = entry.key;
                Field field = entry.value.field;
                if(!NodeHelper.fieldEditable(field)) continue;
                Object childObj = Reflect.get(object, field);
                node.addChild(name, childObj, entry.value);
            }
        }
    }

    public static boolean fieldEditable(Field field){
        int modifiers = field.getModifiers();
        return !Modifier.isFinal(modifiers)
        && !typeBlack(field.getType())
        && !(field.isAnnotationPresent(NoPatch.class) || field.getDeclaringClass().isAnnotationPresent(NoPatch.class));
    }
}
