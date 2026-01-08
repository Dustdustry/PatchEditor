package MinRi2.PatchEditor.node;

import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.JsonValue.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.part.*;
import mindustry.mod.*;

import java.lang.reflect.*;

public class ObjectResolver{
    private static final Seq<Class<?>> resolveBlacklist = Seq.with(
    Prov.class, Class.class, Texture.class, Fi.class, Boolf.class, Func.class,
    DrawPart.class
    );

    // For dynamic editor node
    private static ObjectMap<Class<?>, ObjectNode> templateNode;
    public static final ObjectMap<Class<?>, Object> exampleMap = new ObjectMap<>();

    public static void resolve(ObjectNode node){
        if(node.isRoot()){
            var map = PatchJsonIO.getNameToType();
            for(ContentType ctype : ContentType.all){
                if(map.containsValue(ctype, true)){
                    node.addChild(map.findKey(ctype, true), ctype, ContentType.class, ctype.contentClass, null);
                }
            }

            node.addChild("name", "Patch0", String.class, null, null)
            .addSign(ModifierSign.MODIFY, String.class, null, null);
            return;
        }

        // leaf
        Class<?> objectType = node.type;
        if(objectType == null || objectType.isPrimitive() || objectType.isInterface() || Reflect.isWrapper(objectType)) return;
        if(typeBlack(node.elementType)) return;

        // type resolve
        if(ClassHelper.isArrayLike(objectType)){
            node.addSign(ModifierSign.PLUS, node.type, node.elementType, node.elementType);
        }else if(ClassHelper.isMap(objectType)){
            node.addSign(ModifierSign.PLUS, node.type, node.elementType, node.keyType);
        }

        // object resolve
        Object object = node.object;
        if(object instanceof MapEntry<?,?> entry) object = entry.value;
        if(object != null) objectType = object.getClass();
//        if(object instanceof Block){
//            resolveConsumes(node);
//        }

        if(object instanceof Object[] arr){
            int i = 0;
            for(Object o : arr){
                node.addChild("" + i++, o, node.elementType, null)
                .addSign(ModifierSign.MODIFY, node.type, node.elementType, null);
            }
        }else if(object instanceof Seq<?> seq){
            for(int i = 0; i < seq.size; i++){
                Object o = seq.get(i);
                node.addChild("" + i, o, node.elementType, null)
                .addSign(ModifierSign.MODIFY, node.type, node.elementType, null);
            }
        }else if(object instanceof ObjectSet<?> set){
            int i = 0;
            for(Object o : set){
                node.addChild("" + i++, o, node.elementType, null)
                .addSign(ModifierSign.MODIFY, node.type, node.elementType, null);
            }
        }else if(object instanceof ObjectMap<?, ?> map){
            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                ObjectNode entryNode = node.addChild(name, new MapEntry<>(entry), node.elementType, node.elementType, node.keyType);
                entryNode.addSign(ModifierSign.MODIFY, node.type, node.elementType, node.keyType);
            }
        }else if(object instanceof ContentType ctype){
            OrderedMap<String, Content> map = new OrderedMap<>(); // in order
            for(Content content : Vars.content.getBy(ctype)){
                map.put(PatchJsonIO.getKeyName(content), content);
            }

            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                node.addChild(name, entry.value, null, null);
            }
        }else{
            for(var entry : PatchJsonIO.getFields(objectType)){
                String name = entry.key;
                FieldMetadata fieldMeta = entry.value;
                if(!fieldEditable(fieldMeta.field) || typeBlack(fieldMeta.elementType)) continue;

                Object childObj = object == null ? null : Reflect.get(object, fieldMeta.field);
                ObjectNode child = node.addChild(name, childObj, fieldMeta);

                // no map
                if(!ClassHelper.isMap(child.type)){
                    child.addSign(ModifierSign.MODIFY, child.type, child.elementType, child.keyType);
                }
            }
        }
    }

    public static ObjectNode getTemplate(Class<?> type){
        if(templateNode == null) templateNode = new ObjectMap<>();

        ObjectNode objectNode = templateNode.get(type);
        if(objectNode != null) return objectNode;

        objectNode = new ObjectNode("", getExample(PatchJsonIO.getTypeParser(type), type), type);
        // TODO: template is always modifiable?
        objectNode.addSign(ModifierSign.MODIFY, type, null, null);
        templateNode.put(type, objectNode);
        return objectNode;
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

    public static Object getExample(Class<?> base, Class<?> type){
        if(type.isArray()) return Reflect.newArray(type.getComponentType(), 0);

        type = PatchJsonIO.resolveType(type);

        Object example = exampleMap.get(type);
        if(example != null) return example;

        if(MappableContent.class.isAssignableFrom(type)){
            ContentType contentType = PatchJsonIO.getContentType(type);
            if(contentType != null){
                example = Vars.content.getBy(contentType).first();
            }
        }

        if(example == null){
            JsonValue value = new JsonValue(ValueType.object);
            value.addChild("type", new JsonValue(PatchJsonIO.classTypeName(type)));

            try{
                Json parserJson = PatchJsonIO.getParser().getJson();
                // Invoke internalRead to skip null fields checking.
                example = Reflect.invoke(parserJson, "internalRead", new Object[]{base, null, value, null}, Class.class, Class.class, JsonValue.class, Class.class);
            }catch(Exception ignored){
                return null;
            }
        }

        exampleMap.put(type, example);
        return example;
    }
}
