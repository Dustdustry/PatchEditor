package MinRi2.ContentsEditor.node;

import MinRi2.ContentsEditor.node.modifier.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.Jval.*;
import mindustry.ctype.*;
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

    public static Object readData(EditorNode data){
        if(data.patchNode == null) return null;
        Class<?> type = getTypeIn(data);
        if(type == null) return null;
        return getParser().getJson().readValue(type, data.patchNode.toJson());
    }

    public static String getKeyName(Object object){
        if(object instanceof MappableContent mc) return mc.name;
        if(object instanceof Enum<?> e) return e.name();
        if(object instanceof Class<?> clazz) return clazz.getName();
        return String.valueOf(object);
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

    public static Class<?> getTypeIn(EditorNode node){
        if(node.objectNode != null) return node.objectNode.type;
        if(node.getObject() instanceof MapEntry<?,?> entry) return ClassHelper.unoymousClass(entry.value.getClass());
        return node.getObject() == null ? null : ClassHelper.unoymousClass(node.getObject().getClass());
    }

    /**
     * @return object's class first then meta type.
     */
    public static Class<?> getTypeOut(EditorNode node){
        if(node.getObject() == null) return node.objectNode != null ? node.objectNode.type : null;
        if(node.getObject() instanceof MapEntry<?,?> entry) return ClassHelper.unoymousClass(entry.value.getClass());
        return ClassHelper.unoymousClass(node.getObject().getClass());
    }

    public static ContentType getContentType(Class<?> type){
        if(Block.class.isAssignableFrom(type)) return ContentType.block;
        if(Item.class.isAssignableFrom(type)) return ContentType.item;
        if(Liquid.class.isAssignableFrom(type)) return ContentType.liquid;
        if(StatusEffect.class.isAssignableFrom(type)) return ContentType.status;
        if(UnitType.class.isAssignableFrom(type)) return ContentType.unit;
        return null;
    }

    public static boolean isArray(EditorNode data){
        return ClassHelper.isArray(getTypeOut(data));
    }

    public static boolean isArrayLike(EditorNode data){
        return ClassHelper.isArrayLike(getTypeOut(data));
    }

    public static boolean isMap(EditorNode data){
        return ClassHelper.isMap(getTypeOut(data));
    }

    public static boolean fieldRequired(EditorNode child){
        if(child.objectNode == null) return false;
        Field field = child.objectNode.field;
        if(field == null || field.getType().isPrimitive()) return false;
        if(MappableContent.class.isAssignableFrom(field.getType())){
            return !field.getType().isAnnotationPresent(Nullable.class) && child.getObject() == null;
        }

        return false;
    }

    public static PatchNode parseJson(ObjectNode objectNode, PatchNode patchNode, String patch){
        JsonValue value = getParser().getJson().fromJson(null, Jval.read(patch).toString(Jformat.plain));
        desugarJson(objectNode, value);
        return parseJson(patchNode, value);
    }

    public static PatchNode parseJson(PatchNode patchNode, JsonValue value){
        patchNode.setJson(value);

        for(JsonValue childValue : value){
            PatchNode childNode = patchNode.addChild(childValue.name, childValue.type());
            parseJson(childNode, childValue);
        }

        return patchNode;
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

    private static void desugarJson(ObjectNode node, JsonValue value){
        if(node == null) return;

        Class<?> type = node.type;
        if(type == ItemStack.class || type == PayloadStack.class){
            if(!value.isString() || !value.asString().contains("/")) return;
            String[] split = value.asString().split("/");
            value.setType(ValueType.object);
            addChildValue(value, "item", new JsonValue(split[0]));
            addChildValue(value, "amount", new JsonValue(split[1]));
        }else if(type == LiquidStack.class || type == ConsumeLiquid.class){
            if(!value.isString() || !value.asString().contains("/")) return;
            String[] split = value.asString().split("/");
            value.setType(ValueType.object);
            addChildValue(value, "liquid", new JsonValue(split[0]));
            addChildValue(value, "amount", new JsonValue(split[1]));
        }

        // TODO: More sugar syntaxes support

        for(JsonValue childValue : value){
            desugarJson(node.getOrResolve(childValue.name), childValue);
        }
    }

    public static JsonValue simplifyPatch(JsonValue value){
        int singleCount = 1;
        JsonValue singleEnd = value;
        while(singleEnd.child != null && singleEnd.child.next == null && singleEnd.child.prev == null){
            if(dotSimplifiable(singleEnd)) break;
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

            JsonValue parent = value.parent;
            PatchNode.remove(value);
            addChildValue(parent, name.toString(), singleEnd);
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

    /**
     * Function 'addChild' doesn't set the child's previous jsonValue.
     * see {@link JsonValue}
     */
    public static void addChildValue(JsonValue jsonValue, String name, JsonValue childValue){
        childValue.name = name;
        childValue.parent = jsonValue;

        JsonValue current = jsonValue.child;
        if(current == null){
            jsonValue.child = childValue;
        }else{
            while(true){
                if(current.next == null){
                    current.next = childValue;
                    childValue.prev = current;
                    return;
                }
                current = current.next;
            }
        }
    }
}