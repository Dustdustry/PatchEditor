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

    public static void parseJson(NodeData data, String patch){
        JsonValue value = getParser().getJson().fromJson(null, Jval.read(patch).toString(Jformat.plain));

        data.clearJson();
        parseJson(data, value);
    }

    private static void parseJson(NodeData data, JsonValue value){
        data.setJsonData(value);
        if(value == null || value.isValue()) return;

        if(value.isArray()){
            if(!data.isSign()) return;

            FieldData meta = data.meta;
            if(data.isSign(ModifierSign.PLUS) && meta.elementType != null){
                // copied
                Seq<JsonValue> children = new Seq<>();
                for(JsonValue jsonValue : value){
                    children.add(jsonValue);
                }
                for(JsonValue elemValue : children){
                    NodeData childData = NodeModifier.addCustomChild(data);
                    if(childData == null) return; // getaway
                    childData.setJsonData(elemValue);
                }
            }
            return;
        }

        // extract dot syntax
        JsonIterator iterator = value.iterator();
        while(iterator.hasNext()){
            JsonValue childValue = iterator.next();
            if(childValue.name == null || childValue.isValue()) continue;

            String[] children = childValue.name.split("\\.");
            if(children.length == 1) continue;
            iterator.remove();

            JsonValue current = value;
            for(int i = 0; i < children.length - 1; i++){
                JsonValue extractedValue = new JsonValue(ValueType.object);
                addChildValue(current, children[i], extractedValue);
                current = extractedValue;
            }
            addChildValue(current, children[children.length - 1], childValue);
        }


        for(JsonValue childValue : value){
            String childName = childValue.name;

            // impossible?
            if(childName == null) continue;

            NodeData childData = data.getChild(childName);
            if(childData == null){
                Log.warn("Couldn't resolve @.@", data.name, childName);
                return;
            }

            parseJson(childData, childValue);
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