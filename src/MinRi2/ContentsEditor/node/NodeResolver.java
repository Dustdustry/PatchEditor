package MinRi2.ContentsEditor.node;

import MinRi2.ContentsEditor.node.modifier.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Json.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.part.*;
import mindustry.mod.*;
import mindustry.world.*;

import java.lang.reflect.*;

public class NodeResolver{
    private static final Seq<Class<?>> resolveBlacklist = Seq.with(
    Prov.class, Class.class, Texture.class, TextureRegion.class, Fi.class, Boolf.class, Func.class,
    DrawPart.class
    );

    // v153
    // "item", "liquid", "powerBuffered" are seen as sugar syntax
    private static final String[] consumerKeys = {
        "items", "itemCharged", "itemFlammable", "itemRadioactive", "itemExplosive", "itemList", "itemExplode",
        "liquidFlammable", "liquids", "coolant",
        "power"
    };

    private static final Object empty = new Object();
    private static NodeData current;

    private static void with(NodeData data, Runnable tweaker){
        NodeData last = current;
        current = data;
        tweaker.run();
        current = last;
    }

    private static void markSign(ModifierSign sign){
        markSign(sign, null);
    }

    private static void markSign(ModifierSign sign, FieldData data){
        if(current == null) return;

        FieldData meta = data == null ? current.meta : data;
        current.addChild(sign.sign, meta);
    }

    public static void resolveNode(NodeData node, Object object){
        resolveNode(node, object, PatchJsonIO.getTypeOut(node));
    }

    /**
     * Sign Node's type is array or map.
     */
    private static void resolveNode(NodeData node, @Nullable Object object, Class<?> clazz){
        if(node.isRoot()){
            node.addChild("name", "root", new FieldData(String.class, null, null))
            .addChild(ModifierSign.MODIFY.sign, null);

            var map = PatchJsonIO.getNameToType();
            for(ContentType ctype : ContentType.all){
                if(map.containsValue(ctype, true)){
                    node.addChild(map.findKey(ctype, true), ctype, new FieldData(ContentType.class, ctype.contentClass, null));
                }
            }
            return;
        }

        if(object instanceof MapEntry<?,?> entry){
            object = entry.value;
            clazz = object.getClass();
        }

        if(clazz == null || clazz.isPrimitive() || clazz.isInterface() || Reflect.isWrapper(clazz)) return;
        if(node.meta != null && typeBlack(node.meta.elementType)) return;

        resolveType(node);
        if(!node.isSign() || (node.isSign(ModifierSign.MODIFY) && !PatchJsonIO.isArrayLike(node) && !PatchJsonIO.isMap(node))){
            resolveObject(node, object, clazz);
        }
    }

    private static void resolveType(NodeData node){
        FieldData meta = node.meta;
        if(meta == null) return;

        if(PatchJsonIO.isArrayLike(node)){
            FieldData signMeta = new FieldData(meta.type, meta.elementType, null);
            node.addChild(ModifierSign.PLUS.sign, null, signMeta);
        }else if(PatchJsonIO.isMap(node)){
            FieldData signMeta = new FieldData(meta.elementType, meta.elementType, meta.keyType);
            node.addChild(ModifierSign.PLUS.sign, null, signMeta);
        }
    }

    private static void resolveObject(NodeData node, Object object, Class<?> clazz){
        FieldData meta = node.meta;

        if(object instanceof Block){
            resolveConsumes(node);
        }

        if(object instanceof Object[] arr){
            int i = 0;
            FieldData childMeta = meta == null ? null : new FieldData(meta.elementType);
            for(Object o : arr){
                String name = "" + i++;
                node.addChild(name, o).addChild(ModifierSign.MODIFY.sign, childMeta);
            }
        }else if(object instanceof Seq<?> seq){
            FieldData childMeta = meta == null ? null : new FieldData(meta.elementType);
            for(int i = 0; i < seq.size; i++){
                Object o = seq.get(i);
                node.addChild("" + i, o, childMeta).addChild(ModifierSign.MODIFY.sign, childMeta);
            }
        }else if(object instanceof ObjectSet<?> set){
            int i = 0;
            FieldData childMeta = meta == null ? null : new FieldData(meta.elementType);
            for(Object o : set){
                node.addChild("" + i++, o).addChild(ModifierSign.MODIFY.sign, childMeta);
            }
        }else if(object instanceof ObjectMap<?, ?> map){
            if(meta != null && typeBlack(meta.elementType)) return;

            FieldData childMeta = meta == null ? null : new FieldData(meta.elementType, meta.elementType, meta.keyType);
            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                with(node.addChild(name, new MapEntry<>(entry), childMeta), () -> {
                    markSign(ModifierSign.REMOVE, childMeta);
                    markSign(ModifierSign.MODIFY, childMeta);
                });
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
            for(var entry : PatchJsonIO.getFields(clazz)){
                String name = entry.key;
                FieldMetadata childMeta = entry.value;
                Field field = childMeta.field;
                if(!fieldEditable(field) || typeBlack(childMeta.elementType)) continue;
                Object childObj = object == null ? null : Reflect.get(object, field);
                NodeData child = node.addChild(name, childObj, new FieldData(childMeta));

                // no map
                if(!PatchJsonIO.isMap(child)){
                    child.addChild(ModifierSign.MODIFY.sign, new FieldData(child.meta.type, child.meta.elementType, child.meta.keyType));
                }
            }
        }
    }

    // v153
    private static void resolveConsumes(NodeData node){
        NodeData consumes = node.addChild("consumes", empty);

        NodeData removeData = consumes.addChild("remove", new String[]{}, new FieldData(String[].class));
        with(removeData.addChild("all", false), () -> markSign(ModifierSign.MODIFY));

        for(String key : consumerKeys){
            Class<?> consumerClass = ClassMap.classes.get("Consume" + Strings.capitalize(key));
            if(consumerClass == null){
                Log.err("Consumer class '@' is not found.", "Consume" + Strings.capitalize(key));
                continue;
            }

            Object example = NodeModifier.getExample(consumerClass, consumerClass);
            if(example == null) return;
            consumes.addChild(key, example, new FieldData(consumerClass));
        }
    }

    public static boolean typeBlack(Class<?> clazz){
        return clazz != null && resolveBlacklist.contains(black -> black.isAssignableFrom(clazz));
    }

    public static boolean fieldEditable(Field field){
        int modifiers = field.getModifiers();
        return (!field.getType().isPrimitive() || !Modifier.isFinal(modifiers))
        && !typeBlack(field.getType())
        && !(field.isAnnotationPresent(NoPatch.class) || field.getDeclaringClass().isAnnotationPresent(NoPatch.class));
    }
}
