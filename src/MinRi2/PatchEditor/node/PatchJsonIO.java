package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.patch.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.Jval.*;
import mindustry.ctype.*;
import mindustry.entities.abilities.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.consumers.*;

import java.lang.reflect.*;

public class PatchJsonIO{
    public static final int simplifySingleCount = 3;

    private static ContentParser parser;
    private static ObjectMap<String, ContentType> nameToType;

    /** Classes that is partial when node is dynamic. */
    public static final Seq<Class<?>> partialTypes = Seq.with(
        ItemStack.class, LiquidStack.class, PayloadStack.class
    );

    public static final ObjectMap<Class<?>, Class<?>> defaultClassMap = ObjectMap.of(
        Ability.class, ForceFieldAbility.class
    );

    public static String getKeyName(Object object){
        if(object instanceof MappableContent mc) return mc.name;
        if(object instanceof Enum<?> e) return e.name();
        if(object instanceof Class<?> clazz) return clazz.getName();
        return String.valueOf(object);
    }

    public static ContentParser getParser(){
        if(parser == null) parser = Reflect.get(DataPatcher.class, "parser");
        return parser;
    }

    public static OrderedMap<String, FieldMetadata> getFields(Class<?> type){
        return getParser().getJson().getFields(type);
    }

    public static ObjectMap<String, ContentType> getNameToType(){
        if(nameToType == null) nameToType = Reflect.get(DataPatcher.class, "nameToType");
        return nameToType;
    }

    public static ContentType getContentType(Class<?> type){
        if(Block.class.isAssignableFrom(type)) return ContentType.block;
        if(Item.class.isAssignableFrom(type)) return ContentType.item;
        if(Liquid.class.isAssignableFrom(type)) return ContentType.liquid;
        if(StatusEffect.class.isAssignableFrom(type)) return ContentType.status;
        if(UnitType.class.isAssignableFrom(type)) return ContentType.unit;
        return null;
    }

    public static boolean fieldRequired(EditorNode child){
        if(child.getObjNode() == null) return false;
        Field field = child.getObjNode().field;
        if(field == null || field.getType().isPrimitive()) return false;
        if(MappableContent.class.isAssignableFrom(field.getType())){
            return !field.getType().isAnnotationPresent(Nullable.class) && child.getObject() == null;
        }

        return false;
    }

    public static Class<?> resolveType(Class<?> base, @Nullable String typeJson){
        Class<?> type = typeJson == null ? null : ClassMap.classes.get(typeJson);
        if(type == null) type = base;

        int typeModifiers = type.getModifiers();
        if(!Modifier.isAbstract(typeModifiers) && !Modifier.isInterface(typeModifiers)) return type;

        Class<?> defaultType = defaultClassMap.get(type);
        if(defaultType != null) return defaultType;

        Class<?> finalType = type;
        return ClassMap.classes.values().toSeq().find(c -> {
            int mod = c.getModifiers();
            return !(Modifier.isAbstract(mod) || Modifier.isInterface(mod)) && finalType.isAssignableFrom(c);
        });
    }

    public static int getContainerSize(Object containerLike){
        if(containerLike instanceof Object[] arr) return arr.length;
        if(containerLike instanceof Seq<?> seq) return seq.size;
        if(containerLike instanceof ObjectSet<?> set) return set.size;
        if(containerLike instanceof ObjectMap<?,?> map) return map.size;
        return -1;
    }

    public static void parseJson(ObjectNode objectNode, PatchNode patchNode, String patch){
        JsonValue value = getParser().getJson().fromJson(null, Jval.read(patch).toString(Jformat.plain));
        desugarJson(objectNode, value);
        parseJson(objectNode, patchNode, value);
    }

    public static void parseJson(ObjectNode objectNode, PatchNode patchNode, JsonValue value){
        if(value.isValue()) patchNode.value = value.asString();

        if(value.isArray()){
            int i = 0;

            // override patchNode('array': [])
            // multiple append patchNode(‘+’: [])
            ModifierSign sign = objectNode != null && ClassHelper.isArrayLike(objectNode.type)
            ? (ModifierSign.PLUS.sign.equals(patchNode.key) ? ModifierSign.PLUS : ModifierSign.MODIFY)
            : null;
            for(JsonValue childValue : value){
                ObjectNode childObj = objectNode == null ? null : objectNode.getOrResolve("" + i);
                PatchNode childNode = patchNode.getOrCreate("" + i);
                childNode.sign = sign;

//                if(sign != null) Log.info("'@' got sign @", childNode.buildPath(), childNode.sign);

                parseJson(childObj, childNode, childValue);
            }
            return;
        }

        for(JsonValue childValue : value){
            String name = childValue.name;
            ObjectNode childObj = objectNode == null ? null : objectNode.getOrResolve(name);
            PatchNode childNode = patchNode.getOrCreate(name);

            if(objectNode != null && ClassHelper.isMap(objectNode.type) && objectNode.object instanceof ObjectMap map){
                // patchNode('map': {}) means modify(override) or append key.
                Object key = parser.getJson().readValue(objectNode.keyType, new JsonValue(childValue.name));
                childNode.sign = key != null && map.containsKey(key) ? ModifierSign.MODIFY : ModifierSign.PLUS;
//                Log.info("'@' got sign '@'", childNode.buildPath(), childNode.sign);
            }

            if(objectNode != null && ClassHelper.isArrayLike(objectNode.type)){
                if(!ModifierSign.PLUS.sign.equals(childNode.key)){
                    // patchNode('array': {}) means modify(override)
                    childNode.sign = ModifierSign.MODIFY;
                }else if(childValue.isObject()){
                    // patchNode('+': {}) means single append
                    childNode.sign = ModifierSign.PLUS;
                }
//                Log.info("'@' got sign '@'", childNode.buildPath(), childNode.sign);
            }

            parseJson(childObj, childNode, childValue);
        }
    }

    public static JsonValue toJson(PatchNode patchNode, JsonValue value){
        value.setName(patchNode.key);
        if(patchNode.value != null) value.set(patchNode.value);

        for(Entry<String, PatchNode> entry : patchNode.children){
            JsonValue childValue = new JsonValue(ValueType.object);
            value.addChild(entry.key, childValue);
            toJson(entry.value, childValue);
        }

        return value;
    }

//    public static void parseJson(EditorNode data, String patch){
//        JsonValue value = getParser().getJson().fromJson(null, Jval.read(patch).toString(Jformat.plain));
//
//        data.clearJson();
//        parseJson(data, value);
//    }
//
//    public static void parseJson(EditorNode data, JsonValue value){
//        if(value == null) return;
//
//        desugarJson(data, value);
//        if(value.isValue()){
//            data.setValue(value.asString());
//            return;
//        }
//
//        if(value.isString() && ModifierSign.REMOVE.sign.equals(value.asString())){
//            EditorNode removeSign = data.getSign(ModifierSign.REMOVE);
//            if(removeSign != null) removeSign.initJson();
//            return;
//        }
//
//        if(value.isArray()){
//            if(!isArrayLike(data)) return;
//
//            // If overriding the array, also move children to plusData.
//            EditorNode plusData = data.getSign(ModifierSign.PLUS);
//            if(plusData == null) return;
//
//            data.setPatchNode(value);
//            for(JsonValue elemValue : value){
//                JsonValue typeValue = elemValue.remove("type");
//                Class<?> type = typeValue != null && typeValue.isString() ? ClassMap.classes.get(typeValue.asString()) : null;
//                EditorNode childData = NodeModifier.addDynamicChild(plusData, type);
//                if(childData == null) return; // getaway
//                parseJson(childData, elemValue);
//            }
//            return;
//        }else if(value.has("type")){
//            EditorNode modifyData = data.getSign(ModifierSign.MODIFY);
//            if(modifyData == null){
//                Log.warn("@.@ is unmodifiable.", data.parent.name, data.name);
//                return;
//            }
//
//            Class<?> typeIn = getTypeIn(modifyData);
//            if(typeIn == null){
//                Log.warn("@.@ is unmodifiable.", data.parent.name, data.name);
//                return;
//            }
//
//            JsonValue typeValue = value.remove("type");
//            Class<?> type = typeValue != null && typeValue.isString() ? ClassMap.classes.get(typeValue.asString()) : null;
//            if(type == null || !typeIn.isAssignableFrom(type)){
//                Log.warn("Type '@' is unsustainable to '@'.", type, typeIn);
//                return;
//            }
//
//            EditorNode newData = NodeModifier.changeType(modifyData, type);
//            parseJson(newData, value);
//            return;
//        }
//
//        outer:
//        for(JsonValue childValue : value){
//            // impossible?
//            if(childValue.name == null) continue;
//
//            EditorNode current = data;
//            String[] childNames = childValue.name.split("\\.");
//            for(int i = 0; i < childNames.length; i++){
//                EditorNode childData = current.getChild(childNames[i]);
//                if(childData != null){
//                    current = childData;
//                    continue;
//                }
//
//                // map's key only support in the end
//                if(i == childNames.length - 1 && isMap(current)){
//                    current = parseDynamicChild(current, childNames[i], childValue);
//                    if(current == null) continue outer;
//                    break;
//                }
//
//                Log.warn("Couldn't resolve @.@", current.name, childNames[i]);
//                continue outer;
//            }
//
//            childValue.setName(current.name);
//            parseJson(current, childValue);
//        }
//    }
//
//    private static EditorNode parseDynamicChild(EditorNode data, String childName, JsonValue value){
//        if(isMap(data)){
//            Class<?> keyType = data.meta.keyType;
//
//            EditorNode plusData = data.getChild(ModifierSign.PLUS.sign);
//            Object obj = getParser().getJson().readValue(keyType, new JsonValue(childName));
//            if(obj == null) return null;
//
//            JsonValue typeValue = value.remove("type");
//            Class<?> type = typeValue != null && typeValue.isString() ? ClassMap.classes.get(typeValue.asString()) : null;
//            if(type != null && keyType.isAssignableFrom(type)){
//                Log.warn("Type '@' is unsustainable to '@'.", type, keyType);
//                return null;
//            }
//
//            return NodeModifier.addDynamicChild(plusData, type, childName);
//        }
//
//        return null;
//    }

    private static void desugarJson(ObjectNode objectNode, JsonValue value){
        if(objectNode != null){
            Class<?> type = objectNode.type;
            if(type == ItemStack.class || type == PayloadStack.class){
                if(!value.isString() || !value.asString().contains("/")) return;
                String[] split = value.asString().split("/");
                value.setType(ValueType.object);
                value.addChild("item", new JsonValue(split[0]));
                value.addChild("amount", new JsonValue(split[1]));
            }else if(type == LiquidStack.class || type == ConsumeLiquid.class){
                if(!value.isString() || !value.asString().contains("/")) return;
                String[] split = value.asString().split("/");
                value.setType(ValueType.object);
                value.addChild("liquid", new JsonValue(split[0]));
                value.addChild("amount", new JsonValue(split[1]));
            }
        }

        // extract dot syntax
        if(value.name != null && value.parent != null && value.name.contains(NodeManager.pathComp)){
            String[] names = value.name.split(NodeManager.pathSplitter);

            int i = 0;
            JsonValue currentParent = new JsonValue(ValueType.object);
            currentParent.setName(names[i++]);
            replaceValue(value, currentParent); // don't affect the order

            while(i < names.length - 1){
                currentParent.addChild(names[i++], currentParent = new JsonValue(ValueType.object));
            }

            currentParent.addChild(names[i], value);
        }

        // TODO: More sugar syntaxes support

        for(JsonValue childValue : value){
            desugarJson(objectNode == null ? null : objectNode.getOrResolve(childValue.name), childValue);
        }
    }

    public static JsonValue simplifyPatch(JsonValue value){
        int singleCount = 1;
        JsonValue singleEnd = value;
        while(singleEnd.child != null && singleEnd.child.next == null && singleEnd.child.prev == null){
            if(!dotSimplifiable(singleEnd)) break;
            singleEnd = singleEnd.child;
            singleCount++;
        }

        if(singleCount >= simplifySingleCount){
            StringBuilder name = new StringBuilder();
            JsonValue current = value;
            while(true){
                name.append(current.name);
                current = current.child;
                if(current != singleEnd.child) name.append("."); // dot syntax
                else break;
            }

            singleEnd.setName(name.toString());
            replaceValue(value, singleEnd);
            value = singleEnd;
        }

        sugarJson(value);

        for(JsonValue childValue : value){
            simplifyPatch(childValue);
        }
        return value;
    }

    private static boolean dotSimplifiable(JsonValue singleEnd){
        return !(singleEnd.isArray() || singleEnd.has("type") || singleEnd.name.equals("consumes"));
    }

    private static void sugarJson(JsonValue value){
        if(value.isObject()){
            // duck like
            if(value.has("item") && value.has("amount")){
                value.set(value.get("item").asString() + "/" + value.get("amount").asString());
            }else if(value.has("liquid") && value.has("amount")){
                value.set(value.get("liquid").asString() + "/" + value.get("amount").asString());
            }
        }
    }

    public static void removeJsonValue(JsonValue value){
        JsonValue parent = value.parent, prev = value.prev, next = value.next;

        if(prev != null) prev.next = next;
        else if(parent != null) parent.child = next;

        if(next != null) next.prev = prev;
        value.parent = value.prev = value.next = null;
    }

    public static void replaceValue(JsonValue replaced, JsonValue value){
        if(value.parent != null) removeJsonValue(value);

        JsonValue parent = replaced.parent, prev = replaced.prev, next = replaced.next;

        if(prev != null) prev.next = value;
        else if(parent != null) parent.child = value;

        if(next != null) next.prev = value;

        value.parent = parent;
        value.prev = prev;
        value.next = next;

        replaced.parent = replaced.prev = replaced.next = null;
    }
}