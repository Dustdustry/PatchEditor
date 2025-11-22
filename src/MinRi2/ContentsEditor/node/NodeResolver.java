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
import mindustry.entities.part.*;
import mindustry.mod.*;
import mindustry.mod.ContentPatcher.*;

import java.lang.reflect.*;

public class NodeResolver{
    private static final Seq<Class<?>> resolveBlacklist = Seq.with(
    Prov.class, Class.class, Texture.class, TextureRegion.class, Fi.class, Boolf.class, Func.class,
    DrawPart.class
    );

    public static void resolveNode(NodeData node, Object object){
        resolveFrom(node, object, PatchJsonIO.getTypeOut(node));
    }

    /**
     * Sign Node's type is array or map.
     */
    private static void resolveFrom(NodeData node, @Nullable Object object, Class<?> clazz){
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

        if(node.isSign() && !node.isSign(ModifierSign.MODIFY)) return;
        if(object instanceof MapEntry<?,?> entry){
            object = entry.value;
            clazz = object.getClass();
        }

        if(clazz == null || clazz.isPrimitive() || clazz.isInterface() || Reflect.isWrapper(clazz)) return;

        FieldData meta = node.meta;
        if(meta != null && typeBlack(meta.elementType)) return;

        if(object instanceof Object[] arr){
            int i = 0;
            FieldData childMeta = meta == null ? null : new FieldData(meta.elementType);
            for(Object o : arr){
                String name = "" + i++;
                node.addChild(name, o).addChild(ModifierSign.MODIFY.sign, childMeta);
            }
            FieldData signMeta = meta == null ? null : new FieldData(meta.type, meta.elementType, null);
            node.addChild(ModifierSign.PLUS.sign, null, signMeta);
        }else if(object instanceof Seq<?> seq){
            FieldData childMeta = meta == null ? null : new FieldData(meta.elementType);
            for(int i = 0; i < seq.size; i++){
                Object o = seq.get(i);
                node.addChild("" + i, o, childMeta).addChild(ModifierSign.MODIFY.sign, childMeta);
            }

            FieldData signMeta = meta == null ? null : new FieldData(meta.type, meta.elementType, null);
            node.addChild(ModifierSign.PLUS.sign, signMeta);
        }else if(object instanceof ObjectSet<?> set){
            int i = 0;
            FieldData childMeta = meta == null ? null : new FieldData(meta.elementType);
            for(Object o : set){
                node.addChild("" + i++, o).addChild(ModifierSign.MODIFY.sign, childMeta);
            }

            FieldData signMeta = meta == null ? null : new FieldData(meta.type, meta.elementType, null);
            node.addChild(ModifierSign.PLUS.sign, null, signMeta);
        }else if(object instanceof ObjectMap<?, ?> map){
            if(meta != null && typeBlack(meta.elementType)) return;

            FieldData childMeta = meta == null ? null : new FieldData(meta.elementType, meta.elementType, meta.keyType);
            FieldData signMeta = meta == null ? null : new FieldData(meta.type, meta.elementType, meta.keyType);
            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                NodeData child = node.addChild(name, new MapEntry<>(entry), childMeta);
                if(signMeta != null){
                    child.addChild(ModifierSign.REMOVE.sign, signMeta);
                }
            }
            // unaccessible
            if(!(node.getObject() instanceof Content)){
                node.addChild(ModifierSign.PLUS.sign, childMeta);
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

                // not array or map
                if(child.meta.elementType == null){
                    child.addChild(ModifierSign.MODIFY.sign, child.meta.cpy());
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
