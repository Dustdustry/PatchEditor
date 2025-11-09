package MinRi2.ContentsEditor.node;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.ctype.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;

public class PatchJsonIO{
    public static final int simplifySingleCount = 3;

    private static ContentParser parser;
    private static ObjectMap<String, ContentType> nameToType;

    public static ObjectMap<Class<?>, ContentType> contentClassTypeMap = ObjectMap.of(
        Block.class, ContentType.block,
        Item.class, ContentType.item,
        Liquid.class, ContentType.liquid,
        StatusEffect.class, ContentType.status,
        UnitType.class, ContentType.unit
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
        if(data.jsonData == null) return null;
        Class<?> type = getType(data);
        if(type == null) return null;
        return getParser().getJson().readValue(type, data.jsonData);
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

    public static void parseFrom(NodeData data, JsonValue value){
        data.clearDynamicChildren();

        data.jsonData = value;
        if(value == null || value.isValue()) return;

        for(JsonValue childValue : value){
            String childName = childValue.name;

            if(childName == null){
                if(childValue.isArray()){
                    // TODO
                }
                continue;
            }

            NodeData current = data;
            JsonValue currentValue = value;

            String[] childrenName = childName.split("\\.");
            for(String name : childrenName){
                NodeData childData = current.getChild(name);
                if(childData == null){
                    Log.warn("Couldn't resolve @.@", current.name, name);
                    return;
                }

                currentValue = value.get(name);
                current = childData;
            }

            parseFrom(current, currentValue);
        }
    }

    public static JsonValue processPatch(JsonValue value){
        if(ModifierSign.PLUS.sign.equals(value.name)){
            JsonValue fieldData = value.parent;
            fieldData.remove(value.name);

            JsonValue parent = fieldData.parent;
            if(fieldData.size == 0) parent.remove(fieldData.name);

            addChildValue(parent, fieldData.name + "." + value.name, value);
        }

        for(JsonValue child : value){
            processPatch(child);
        }
        return value;
    }

    public static JsonValue simplifyPatch(JsonValue value){
        int singleCount = 0;
        JsonValue singleEnd = value;
        while(singleEnd.child != null && singleEnd.size == 1){
            singleEnd = singleEnd.child;
            singleCount++;
        }

        if(singleCount >= simplifySingleCount){
            StringBuilder name = new StringBuilder();
            JsonValue current = value;
            while(true){
                name.append(current.name);
                current = current.child;
                if(current != null) name.append("."); // dot syntax
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