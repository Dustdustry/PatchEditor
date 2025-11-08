package MinRi2.ContentsEditor.node;

import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
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
                    node.addChild(ctype.toString().toLowerCase(Locale.ROOT), ctype, new FieldData(ContentType.class, ctype.contentClass, null));
                }
            }
        }else if(object instanceof Object[] arr){
            if(node.meta != null && typeBlack(node.meta.elementType)) return;

            int i = 0;
            for(Object o : arr){
                String name = "" + i++;
                node.addChild(name, o);
            }
            node.addChild(ModifierSign.PLUS.sign, null, node.meta); // extend field meta
        }else if(object instanceof Seq<?> seq){
            if(node.meta != null && typeBlack(node.meta.elementType)) return;

            for(int i = 0; i < seq.size; i++){
                Object o = seq.get(i);
                node.addChild("" + i, o);
            }
            node.addChild(ModifierSign.PLUS.sign, null, node.meta);
        }else if(object instanceof ObjectSet<?> set){
            if(node.meta != null && typeBlack(node.meta.elementType)) return;

            int i = 0;
            for(Object o : set){
                node.addChild("" + i++, o);
            }
            node.addChild(ModifierSign.PLUS.sign, null);
        }else if(object instanceof ObjectMap<?, ?> map){
            if(node.meta != null && typeBlack(node.meta.elementType)) return;

            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                NodeData child = node.addChild(name, entry.value);
                child.addChild(ModifierSign.REMOVE.sign, null, node.meta);
            }
            // unaccessible
            if(!(node.getObject() instanceof Content)){
                node.addChild(ModifierSign.PLUS.sign, null, node.meta);
            }
        }else if(object instanceof ContentType ctype){
            OrderedMap<String, Content> map = new OrderedMap<>(); // in order
            for(Content content : Vars.content.getBy(ctype)){
                map.put(PatchJsonIO.getKeyName(content), content);
            }
            resolveFrom(node, map);
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
