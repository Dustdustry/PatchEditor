package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.EditorList.*;
import arc.files.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.JsonValue.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;

public class ObjectResolver{
    private static final Seq<Class<?>> classBlacklist = Seq.with(
    Class.class, Texture.class, Fi.class, KeyBind.class, UnitEntity.class
    );
    private static final ObjectMap<Class<?>, Seq<String>> fieldBlacklist = ObjectMap.of(
    Drill.class, Seq.with("oreCount", "itemArray"),
    UnitType.class, Seq.with("sample")
    );

    // For dynamic editor node
    private static ObjectMap<Class<?>, ObjectNode> templateNode;
    private static final ObjectMap<Class<?>, Object> exampleMap = new ObjectMap<>();

    public static void resolve(ObjectNode node){
        if(node.isRoot()){
            var map = PatchJsonIO.getNameToType();
            for(ContentType ctype : ContentType.all){
                if(map.containsValue(ctype, true)){
                    node.addChild(map.findKey(ctype, true), ctype, ContentType.class, ctype.contentClass, null);
                }
            }

            node.addChild("name", "Patch0").addSign(ModifierSign.MODIFY);
            return;
        }

        Class<?> objectType = node.type;
        Object object = node.object;
        if(object == null && objectType == null) return;

        if(object != null){
            if(object instanceof MapEntry<?, ?> entry) object = entry.value;
            objectType = object.getClass();
        }else{
            object = getExample(objectType, objectType);
        }

        // type resolve
        if(!typeResolvable(objectType)) return;
        if(node.elementType != null && !typeEditable(node.elementType)) return;

        if(ClassHelper.isArrayLike(objectType)){
            node.addSign(ModifierSign.PLUS, null, node.elementType, null);
        }else if(ClassHelper.isMap(objectType)){
            node.addSign(ModifierSign.PLUS, null, node.elementType, node.keyType);
        }

        // object resolve
        if(object instanceof Object[] arr){
            int i = 0;
            for(Object o : arr){
                if(o == null) continue;
                node.addChild("" + i++, o, node.elementType)
                .addSign(ModifierSign.MODIFY, node.type, node.elementType, null);
            }
        }else if(object instanceof Seq<?> seq){
            for(int i = 0; i < seq.size; i++){
                Object o = seq.get(i);
                node.addChild("" + i, o, node.elementType)
                .addSign(ModifierSign.MODIFY, node.type, node.elementType, null);
            }
        }else if(object instanceof ObjectSet<?> set){
            int i = 0;
            for(Object o : set){
                node.addChild("" + i++, o, node.elementType)
                .addSign(ModifierSign.MODIFY, node.type, node.elementType, null);
            }
        }else if(object instanceof ObjectMap<?, ?> map){
            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                ObjectNode entryNode = node.addChild(name, new MapEntry<>(entry), node.elementType, node.elementType, node.keyType);
                entryNode.addSign(ModifierSign.MODIFY);
            }
        }else if(object instanceof ObjectFloatMap<?> map){
            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                ObjectNode entryNode = node.addChild(name, new MapEntry<>(entry.key, entry.value), node.elementType, node.elementType, node.keyType);
                entryNode.addSign(ModifierSign.MODIFY);
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
        }else if(object instanceof EnumSet<?> set){
            Class<?> enumClass = set.array.getClass().getComponentType();
            if(enumClass == null){
                Log.warn("Cannot get enum type of enum set ''.", node.name);
                return;
            }
            int i = 0;
            for(Object o : set.array){
                node.addChild("" + i++, o, enumClass)
                .addSign(ModifierSign.MODIFY, enumClass, enumClass, null);
            }
        }else if(object instanceof Attributes attributes){
            for(Attribute att : Attribute.all){
                node.addChild(att.name, attributes.get(att)).addSign(ModifierSign.MODIFY);
            }
        }else{
            resolveFields(node, object, objectType);
        }
    }

    private static void resolveFields(ObjectNode node, Object object, Class<?> objectType){
        Seq<String> blacklist = findFieldBlacklist(objectType);
        for(var entry : PatchJsonIO.getFields(objectType)){
            String name = entry.key;
            if(blacklist != null && blacklist.contains(name)) continue;

            FieldMetadata fieldMeta = entry.value;
            if(!fieldResolvable(fieldMeta.field)) continue;
            if(fieldMeta.elementType != null && !typeResolvable(fieldMeta.elementType)) continue;

            FieldMetadata copiedMeta = new FieldMetadata(fieldMeta.field);
            Object childObj = object == null ? null : Reflect.get(object, fieldMeta.field);
            // fix meta
            if(childObj instanceof ObjectFloatMap<?>){
                copiedMeta.keyType = copiedMeta.elementType;
                copiedMeta.elementType = float.class;
            }

            ObjectNode child = node.addChild(name, childObj, copiedMeta);

            // no map
            if(!ClassHelper.isMap(child.type)){
                child.addSign(ModifierSign.MODIFY);
            }
        }

        // specific fields
        if(object instanceof UnitType type){
            // classTypeName will be resolved to prov
            node.addChild("type", EditorList.getUnitTypeName(type.constructor.get().getClass()), UnitConstructorType.class).addSign(ModifierSign.MODIFY);
            node.addChild("aiController", PatchJsonIO.getClassTypeName(type.aiController.get().getClass()), AIController.class).addSign(ModifierSign.MODIFY);
            node.addChild("controller", PatchJsonIO.getClassTypeName(CommandAI.class), AIController.class).addSign(ModifierSign.MODIFY);
        }

        if(object instanceof Block){
            ObjectNode consumesNode = node.addChild("consumes", null, Consume.class);

            consumesNode.addSign(ModifierSign.MODIFY);

            consumesNode.addChild("item", null, Item.class).addSign(ModifierSign.MODIFY);
            consumesNode.addChild("itemCharged", null, ConsumeItemCharged.class).addSign(ModifierSign.MODIFY);
            consumesNode.addChild("itemFlammable", null, ConsumeItemFlammable.class).addSign(ModifierSign.MODIFY);
            consumesNode.addChild("itemRadioactive", null, ConsumeItemRadioactive.class).addSign(ModifierSign.MODIFY);
            consumesNode.addChild("itemExplosive", null, ConsumeItemExplosive.class).addSign(ModifierSign.MODIFY);
            consumesNode.addChild("itemList", null, ConsumeItemList.class).addSign(ModifierSign.MODIFY);
            consumesNode.addChild("itemExplode", null, ConsumeItemExplode.class).addSign(ModifierSign.MODIFY);
//            consumesNode.addChild("items") // TODO: desugar string and array

            consumesNode.addChild("liquidFlammable", null, ConsumeLiquidFlammable.class).addSign(ModifierSign.MODIFY);
            consumesNode.addChild("liquid", null, ConsumeLiquid.class).addSign(ModifierSign.MODIFY);
//            consumesNode.addChild("liquids", null, ) // TODO: desugar string and array
            consumesNode.addChild("coolant", null, ConsumeCoolant.class).addSign(ModifierSign.MODIFY);
            consumesNode.addChild("power", null, ConsumePower.class).addSign(ModifierSign.MODIFY); // TODO: desugar number
            consumesNode.addChild("powerBuffered", null, float.class).addSign(ModifierSign.MODIFY);
        }
    }

    private static Seq<String> findFieldBlacklist(Class<?> type){
        Class<?> current = type;
        while(current != Object.class){
            Seq<String> list = fieldBlacklist.get(current);
            if(list != null) return list;
            current = current.getSuperclass();
        }

        return null;
    }

    public static ObjectNode getShadowNode(ObjectNode node, Class<?> newType){
        ObjectNode shadowNode = new ObjectNode(node.name, getExample(newType, newType), node.field, node.type, node.elementType, node.keyType);
        if(node.hasSign(ModifierSign.MODIFY)){
            shadowNode.addSign(ModifierSign.MODIFY);
        }
        return shadowNode;
    }

    public static ObjectNode getTemplate(Class<?> type){
        if(templateNode == null) templateNode = new ObjectMap<>();

        ObjectNode objectNode = templateNode.get(type);
        if(objectNode != null) return objectNode;

        objectNode = new ObjectNode("", getExample(PatchJsonIO.resolveType(type), type), type);
        // TODO: template is always modifiable?
        objectNode.addSign(ModifierSign.MODIFY);
        templateNode.put(type, objectNode);
        return objectNode;
    }

    public static boolean typeResolvable(Class<?> clazz){
        return clazz != null && !(clazz.isPrimitive() || Reflect.isWrapper(clazz))
        && !ClassHelper.isAbstractClass(clazz) && typeEditable(clazz);
    }

    public static boolean typeEditable(Class<?> clazz){
        return clazz != null && (!clazz.isInterface() || clazz == Interp.class)
        && !(clazz.isSynthetic() || classBlacklist.contains(black -> black.isAssignableFrom(clazz)));
    }

    public static boolean fieldResolvable(Field field){
        int modifiers = field.getModifiers();
        return (!field.getType().isPrimitive() || !Modifier.isFinal(modifiers))
        && typeEditable(field.getType())
        && !(field.isAnnotationPresent(NoPatch.class) || field.getDeclaringClass().isAnnotationPresent(NoPatch.class));
    }

    public static Object getExample(Class<?> base, Class<?> type){
        if(type == float.class) return 0f; // add more primitive cases if necessary
        if(type == int.class || type == short.class) return 0;
        if(type.isArray()) return Reflect.newArray(type.getComponentType(), 0);
        // TODO: example map?

        type = PatchJsonIO.resolveType(type);

        Object example = exampleMap.get(type);
        if(example != null) return example;

        if(MappableContent.class.isAssignableFrom(type)){
            ContentType contentType = PatchJsonIO.classContentType(type);
            if(contentType != null){
                example = Vars.content.getBy(contentType).first();
            }
        }

        if(example == null){
            base = PatchJsonIO.getTypeParser(base);
            JsonValue value = new JsonValue(ValueType.object);
            value.addChild("type", new JsonValue(PatchJsonIO.getClassTypeName(type)));

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
