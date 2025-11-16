package MinRi2.ContentsEditor.node;

import MinRi2.ContentsEditor.node.modifier.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.Jval.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.consumers.*;

public class PatchJsonIO{
    public static final int simplifySingleCount = 3;

    private static ContentParser parser;
    private static ObjectMap<String, ContentType> nameToType;

    public static final ObjectMap<Class<?>, ContentType> contentClassTypeMap = ObjectMap.of(
        Block.class, ContentType.block,
        Item.class, ContentType.item,
        Liquid.class, ContentType.liquid,
        StatusEffect.class, ContentType.status,
        UnitType.class, ContentType.unit
    );

    /** Classes that type is partial when node is dynamic. */
    public static final Seq<Class<?>> partialTypeClass = Seq.with(
        ItemStack.class, LiquidStack.class, PayloadStack.class
    );

    public static boolean isArray(NodeData data){
        return isArray(getType(data));
    }

    public static boolean isMap(NodeData data){
        return isMap(getType(data));
    }

    public static boolean isArray(Class<?> type){
        return type != null && (type.isArray() || Seq.class.isAssignableFrom(type) || ObjectSet.class.isAssignableFrom(type));
    }

    public static boolean isMap(Class<?> type){
        return type != null && ObjectMap.class.isAssignableFrom(type);
    }

    public static Object readData(NodeData data){
        if(data.getJsonData() == null) return null;
        Class<?> type = getType(data);
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

    public static Class<?> getType(NodeData node){
        if(node.meta != null) return node.meta.type;
        if(node.getObject() == null) return null;
        Class<?> clazz = node.getObject().getClass();
        while(clazz.isAnonymousClass()) clazz = clazz.getSuperclass();
        return clazz;
    }

    public static void parseJson(NodeData data, String patch){
        JsonValue value = getParser().getJson().fromJson(null, Jval.read(patch).toString(Jformat.plain));

        data.clearJson();
        parseJson(data, value);
    }

    private static void parseJson(NodeData data, JsonValue value){
        if(value != null) desugarJson(data, value);
        if(value == null || value.isValue()){
            data.setJsonData(value);
            return;
        }

        if(value.isArray()){
            data.setJsonData(value);
            if(!data.isSign()) return;

            FieldData meta = data.meta;
            if(data.isSign(ModifierSign.PLUS) && meta.elementType != null){
                for(JsonValue elemValue : value){
                    JsonValue typeValue = elemValue.remove("type");
                    String typeName = typeValue != null && typeValue.isString() ? typeValue.asString() : null;
                    NodeData childData = NodeModifier.addCustomChild(data, typeName);
                    if(childData == null) return; // getaway
                    parseJson(childData, elemValue);
                }
            }
            return;
        }

        outer:
        for(JsonValue childValue : value){
            // impossible?
            if(childValue.name == null) continue;

            NodeData current = data;
            for(String name : childValue.name.split("\\.")){
                NodeData childData = current.getChild(name);
                if(childData == null){
                    Log.warn("Couldn't resolve @.@", current.name, name);
                    current.clearJson();
                    continue outer;
                }
                current = childData;
            }

            childValue.setName(current.name);
            parseJson(current, childValue);
        }
    }

    private static void desugarJson(NodeData data, JsonValue value){
        Class<?> type = getType(data);
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

    public static JsonValue toJson(NodeData data){
        JsonValue jsonData = data.getJsonData();
        if(jsonData == null) return new JsonValue(ValueType.object);
        return processPatch(toJson(data, new JsonValue(jsonData.type())));
    }

    private static JsonValue toJson(NodeData node, JsonValue json){
        JsonValue data = node.getJsonData();
        if(data == null) return json;
        json.setName(data.name);

        if(data.isValue()){
            json.set(data.asString());
            return json;
        }

        processData(node, json);

        for(NodeData child : node.getChildren()){
            JsonValue childData = child.getJsonData();
            if(childData == null) continue;
            JsonValue childJson = toJson(child, new JsonValue(childData.type()));
            addChildValue(json, childJson.name, childJson);
        }

        return json;
    }

    private static void processData(NodeData node, JsonValue value){
        if(node.parentData != null && node.parentData.isSign(ModifierSign.PLUS)){
            Class<?> type = getType(node);
            if(type == null || partialTypeClass.contains(type)) return;
            String typeName = ClassMap.classes.findKey(type, true);
            if(typeName == null) typeName = type.getCanonicalName();
            addChildValue(value, "type", new JsonValue(typeName));
        }
    }

    private static JsonValue processPatch(JsonValue value){
        // plus syntax must be used in dot syntax
        if(ModifierSign.PLUS.sign.equals(value.name)){
            JsonValue fieldData = value.parent;
            fieldData.remove(value.name);

            JsonValue parent = fieldData.parent;
            if(fieldData.child == null) parent.remove(fieldData.name);

            addChildValue(parent, fieldData.name + "." + value.name, value);
        }

        for(JsonValue child : value){
            processPatch(child);
        }

        return value;
    }

    public static JsonValue simplifyPatch(JsonValue value){
        int singleCount = 1;
        JsonValue singleEnd = value;
        while(singleEnd.isObject() && singleEnd.child != null && singleEnd.child.next == null && singleEnd.child.prev == null){
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
            parent.remove(value.name);
            addChildValue(parent, name.toString(), singleEnd);
            return singleEnd;
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
}