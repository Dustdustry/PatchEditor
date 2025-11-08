package MinRi2.ContentsEditor.node;

import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Json.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.mod.*;

import java.lang.reflect.*;
import java.util.*;

public class NodeResolver{
    private static final Seq<Class<?>> resolveBlacklist = Seq.with(
    Prov.class, Class.class, Texture.class, TextureRegion.class, Fi.class, Boolf.class
    );

    public static void resolveFrom(NodeData node, Object object){
        if(object == null) return; // ignore null?

        if(object == NodeHelper.getRootObj()){
            node.addChild("name", "root", new FieldData(String.class, null, null))
            .addChild(ModifierSign.MODIFY.sign, null);
            var map = NodeHelper.getNameToType();
            for(ContentType ctype : ContentType.all){
                if(map.containsValue(ctype, true)){
                    node.addChild(map.findKey(ctype, true), ctype, new FieldData(ContentType.class, ctype.contentClass, null));
                }
            }
            return;
        }

        if(object instanceof MapEntry<?,?> entry){
            object = entry.value;
        }

        FieldData meta = node.meta;
        if(object instanceof Object[] arr){
            if(meta != null && typeBlack(meta.elementType)) return;

            int i = 0;
            for(Object o : arr){
                String name = "" + i++;
                node.addChild(name, o);
            }
            node.addChild(ModifierSign.PLUS.sign, null, meta); // extend field meta
        }else if(object instanceof Seq<?> seq){
            if(meta != null && typeBlack(meta.elementType)) return;

            for(int i = 0; i < seq.size; i++){
                Object o = seq.get(i);
                node.addChild("" + i, o);
            }

            FieldData signMeta = meta == null ? null : new FieldData(null, meta.elementType, null);
            node.addChild(ModifierSign.PLUS.sign, null, signMeta);
        }else if(object instanceof ObjectSet<?> set){
            if(meta != null && typeBlack(meta.elementType)) return;

            int i = 0;
            for(Object o : set){
                node.addChild("" + i++, o);
            }

            FieldData signMeta = meta == null ? null : new FieldData(null, meta.elementType, meta.keyType);
            node.addChild(ModifierSign.PLUS.sign, signMeta);
        }else if(object instanceof ObjectMap<?, ?> map){
            if(meta != null && typeBlack(meta.elementType)) return;

            FieldData childMeta = meta == null ? null : new FieldData(meta.elementType, meta.elementType, meta.keyType);
            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                NodeData child = node.addChild(name, new MapEntry<>(entry), childMeta);
                if(meta != null){
                    child.addChild(ModifierSign.REMOVE.sign, null, childMeta);
                }
            }
            // unaccessible
            if(!(node.getObject() instanceof Content)){
                node.addChild(ModifierSign.PLUS.sign, null, childMeta);
            }
        }else if(object instanceof ContentType ctype){
            OrderedMap<String, Content> map = new OrderedMap<>(); // in order
            for(Content content : Vars.content.getBy(ctype)){
                map.put(PatchJsonIO.getKeyName(content), content);
            }

            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                node.addChild(name, entry.value);
            }
        }else{
            for(var entry : NodeHelper.getFields(object.getClass())){
                String name = entry.key;
                Field field = entry.value.field;
                if(!fieldEditable(field)) continue;
                Object childObj = Reflect.get(object, field);
                NodeData child = node.addChild(name, childObj, new FieldData(entry.value));

                // not array or map
                if(child.meta.elementType == null){
                    child.addChild(ModifierSign.MODIFY.sign, null, child.meta);
                }
            }
        }
    }

    public static boolean typeBlack(Class<?> clazz){
        return clazz != null && resolveBlacklist.contains(black -> black.isAssignableFrom(clazz));
    }

    public static boolean fieldEditable(Field field){
        int modifiers = field.getModifiers();
        return !Modifier.isFinal(modifiers)
        && !typeBlack(field.getType())
        && !(field.isAnnotationPresent(NoPatch.class) || field.getDeclaringClass().isAnnotationPresent(NoPatch.class));
    }
}
