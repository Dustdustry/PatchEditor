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

    public static Object readData(NodeData data){
        if(data.getJsonData() == null) return null;
        Class<?> type = getTypeIn(data);
        if(type == null) return null;
        return getParser().getJson().readValue(type, data.getJsonData());
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

    public static Class<?> getTypeIn(NodeData node){
        if(node.meta != null) return node.meta.type;
        if(node.getObject() instanceof MapEntry<?,?> entry) return ClassHelper.unoymousClass(entry.value.getClass());
        return node.getObject() == null ? null : ClassHelper.unoymousClass(node.getObject().getClass());
    }

    /**
     * @return object's class first then meta type.
     */
    public static Class<?> getTypeOut(NodeData node){
        if(node.getObject() == null) return node.meta != null ? node.meta.type : null;
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

    public static boolean isArray(NodeData data){
        return ClassHelper.isArray(getTypeOut(data));
    }

    public static boolean isArrayLike(NodeData data){
        return ClassHelper.isArrayLike(getTypeOut(data));
    }

    public static boolean isMap(NodeData data){
        return ClassHelper.isMap(getTypeOut(data));
    }

    public static boolean fieldRequired(NodeData child){
        if(child.meta == null) return false;
        Field field = child.meta.field;
        if(field == null || field.getType().isPrimitive()) return false;
        if(MappableContent.class.isAssignableFrom(field.getType())){
            return !field.getType().isAnnotationPresent(Nullable.class) && child.getObject() == null;
        }

        return false;
    }

    public static void parseJson(NodeData data, String patch){
        JsonValue value = getParser().getJson().fromJson(null, Jval.read(patch).toString(Jformat.plain));

        data.clearJson();
        parseJson(data, value);
    }

    public static void parseJson(NodeData data, JsonValue value){
        if(value != null) desugarJson(data, value);
        if(value == null || value.isValue()){
            data.setJsonData(value);
            return;
        }

        if(value.isArray()){
            if(!isArrayLike(data)) return;

            data.setJsonData(value);
            for(JsonValue elemValue : value){
                JsonValue typeValue = elemValue.remove("type");
                Class<?> type = typeValue != null && typeValue.isString() ? ClassMap.classes.get(typeValue.asString()) : null;
                NodeData childData = NodeModifier.addDynamicChild(data, type);
                if(childData == null) return; // getaway
                parseJson(childData, elemValue);
            }
            return;
        }else if(value.has("type")){
            NodeData modifyData = data.getSign(ModifierSign.MODIFY);
            if(modifyData == null){
                Log.warn("@.@ is unmodifiable.", data.parentData.name, data.name);
                return;
            }

            Class<?> typeIn = getTypeIn(modifyData);
            if(typeIn == null){
                Log.warn("@.@ is unmodifiable.", data.parentData.name, data.name);
                return;
            }

            JsonValue typeValue = value.remove("type");
            Class<?> type = typeValue != null && typeValue.isString() ? ClassMap.classes.get(typeValue.asString()) : null;
            if(type == null || !typeIn.isAssignableFrom(type)){
                Log.warn("Type '@' is unsustainable to '@'.", type, typeIn);
                return;
            }

            NodeData newData = NodeModifier.changeType(modifyData, type);
            parseJson(newData, value);
            return;
        }

        outer:
        for(JsonValue childValue : value){
            // impossible?
            if(childValue.name == null) continue;

            NodeData current = data;
            String[] childNames = childValue.name.split("\\.");
            for(int i = 0; i < childNames.length; i++){
                NodeData childData = current.getChild(childNames[i]);
                if(childData != null){
                    current = childData;
                    continue;
                }

                // map's key only support when in the end
                if(i == childNames.length - 1 && isMap(current)){
                    current = parseDynamicChild(current, childNames[i], childValue);
                    if(current == null) continue outer;
                    break;
                }

                Log.warn("Couldn't resolve @.@", current.name, childNames[i]);
                continue outer;
            }

            childValue.setName(current.name);
            parseJson(current, childValue);
        }
    }

    private static NodeData parseDynamicChild(NodeData data, String childName, JsonValue value){
        if(isMap(data)){
            Class<?> keyType = data.meta.keyType;

            NodeData plusData = data.getChild(ModifierSign.PLUS.sign);
            Object obj = getParser().getJson().readValue(keyType, new JsonValue(childName));
            if(obj == null) return null;

            JsonValue typeValue = value.remove("type");
            Class<?> type = typeValue != null && typeValue.isString() ? ClassMap.classes.get(typeValue.asString()) : null;
            if(type != null && keyType.isAssignableFrom(type)){
                Log.warn("Type '@' is unsustainable to '@'.", type, keyType);
                return null;
            }

            return NodeModifier.addDynamicChild(plusData, type, childName);
        }

        return null;
    }

    private static void desugarJson(NodeData data, JsonValue value){
        Class<?> type = getTypeOut(data);
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
    }

    public static JsonValue toJson(NodeData node){
        JsonValue jsonData = node.getJsonData();
        if(jsonData == null) return new JsonValue(ValueType.object);
        JsonValue linkedValue = linkJsonData(node, new JsonValue(jsonData.type()));
        processData(node, linkedValue);
        return linkedValue;
    }

    private static JsonValue linkJsonData(NodeData node, JsonValue json){
        JsonValue data = node.getJsonData();
        if(data == null) return json;

        if(data.isValue()){
            json.set(data.asString());
            return json;
        }

        for(NodeData child : node.getChildren()){
            JsonValue childData = child.getJsonData();
            if(childData == null) continue;
            JsonValue childJson = new JsonValue(childData.type());
            addChildValue(json, child.name, childJson);
            linkJsonData(child, childJson);
        }

        return json;
    }

    private static void processData(NodeData node, JsonValue value){
        for(JsonValue childValue : value){
            NodeData child = node.getChild(childValue.name);
            if(child != null) processData(child, childValue);
        }

        if(value.type() == ValueType.object && (node.isSign(ModifierSign.MODIFY) || node.isDynamic())){
            Class<?> type = getTypeOut(node);
            if(type != null && !partialTypes.contains(type)){
                String typeName = ClassMap.classes.findKey(type, true);
                if(typeName == null) typeName = type.getName();
                addChildValue(value, "type", new JsonValue(typeName));
            }
        }

        if(node.isSign(ModifierSign.MODIFY)){
            JsonValue effectValue = value.parent;
            JsonValue effectParentValue = effectValue.parent;
            removeValue(effectValue);
            addChildValue(effectParentValue, effectValue.name, value);
        }else if(node.isSign(ModifierSign.PLUS)){
            JsonValue effectValue = value.parent;
            JsonValue effectParentValue = effectValue.parent;

            if(isMap(node.parentData)){
                removeValue(value);
                addChildValue(effectValue, value.child.name, value.child);
            }else{
                removeValue(value);
                // clean empty object
                if(effectValue.child == null) removeValue(effectValue);
                // plus syntax must be used in dot syntax
                addChildValue(effectParentValue, effectValue.name + "." + value.name, value);
            }
        }
    }

    public static JsonValue simplifyPatch(JsonValue value){
        int singleCount = 1;
        JsonValue singleEnd = value;
        while(singleEnd.child != null && singleEnd.child.next == null && singleEnd.child.prev == null){
            if(singleEnd.isArray() || singleEnd.has("type")) break;
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
            removeValue(value);
            addChildValue(parent, name.toString(), singleEnd);
            value = singleEnd;
        }

        if(value.isObject()){
            // duck like
            if(value.has("item") && value.has("amount")){
                value.set(value.get("item").asString() + "/" + value.get("amount").asString());
            }else if(value.has("liquid") && value.has("amount")){
                value.set(value.get("liquid").asString() + "/" + value.get("amount").asString());
            }
        }

        for(JsonValue child : value){
            simplifyPatch(child);
        }
        return value;
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

    public static void removeValue(JsonValue value){
        JsonValue prev = value.prev, next = value.next;
        if(prev != null) prev.next = next;
        else value.parent.child = next;
        if(next != null) next.prev = prev;
        value.parent = value.prev = value.next = null;
    }
}