package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.patch.*;
import arc.struct.*;
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
import java.util.*;

public class PatchJsonIO{
    public static final boolean debug = true;

    public static final int simplifySingleCount = 3;

    private static ContentParser parser;
    private static ObjectMap<String, ContentType> nameToType;

    public static final String appendPrefix = "#ADD_";

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
        return resolveType(typeJson == null ? base : ClassMap.classes.get(typeJson));
    }

    public static Class<?> resolveType(Class<?> type){
        if(type.isPrimitive() || ClassHelper.isArray(type)) return type;

        int typeModifiers = type.getModifiers();
        if(!Modifier.isAbstract(typeModifiers) && !Modifier.isInterface(typeModifiers)) return type;

        Class<?> defaultType = defaultClassMap.get(type);
        if(defaultType != null) return defaultType;

        return ClassMap.classes.values().toSeq().find(c -> {
            int mod = c.getModifiers();
            return !(Modifier.isAbstract(mod) || Modifier.isInterface(mod)) && type.isAssignableFrom(c);
        });
    }

    public static String classTypeName(Class<?> type){
        String name = ClassMap.classes.findKey(type, true);
        if(name == null) name = type.getName();
        return name;
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
        extractDotSyntax(value);
        desugarJson(objectNode, value);
        parseJson(objectNode, patchNode, value);
    }

    public static void parseJson(ObjectNode objectNode, PatchNode patchNode, JsonValue value){
        if(value.isValue()) patchNode.value = value.asString();
        patchNode.type = value.type();

        if(value.isArray()){
            // patchNode('array': []) -> override array.
            int i = 0;
            for(JsonValue childValue : value){
                PatchNode childNode = patchNode.getOrCreate("" + i++);
                childNode.sign = ModifierSign.PLUS;
                parseJson(null, childNode, childValue);
            }
            patchNode.sign = ModifierSign.MODIFY;
            return;
        }

        // sign is seen as attribute in PatchNode, not a node
        if(value.has(ModifierSign.PLUS.sign)){
            JsonValue plusValue = value.remove(ModifierSign.PLUS.sign);

            int i = objectNode != null ? getContainerSize(objectNode.object) : 0;
            if(plusValue.isArray()){
                // patchNode(‘+’: []) -> multiple append
                for(JsonValue childValue : plusValue){
                    PatchNode childNode = patchNode.getOrCreate(appendPrefix + i++);
                    childNode.sign = ModifierSign.PLUS;
                    if(debug) Log.info("'@' got sign @", childNode.getPath(), childNode.sign);
                    parseJson(null, childNode, childValue);
                }
            }else if(plusValue.isObject()){
                // patchNode('+': {}) -> single append
                PatchNode childNode = patchNode.getOrCreate(appendPrefix + i);
                childNode.sign = ModifierSign.PLUS;
                if(debug) Log.info("'@' got sign @", childNode.getPath(), childNode.sign);
                parseJson(null, childNode, plusValue);
            }
        }

        for(JsonValue childValue : value){
            String name = childValue.name;

            ObjectNode childObj = objectNode == null ? null : objectNode.getOrResolve(name);
            PatchNode childNode = patchNode.getOrCreate(name);

            if(objectNode != null && ClassHelper.isMap(objectNode.type) && objectNode.object instanceof ObjectMap map){
                // patchNode('map': {}) -> modify(override) or append key
                Object key = parser.getJson().readValue(objectNode.keyType, new JsonValue(childValue.name));
                if(key != null && !map.containsKey(key)){
                    childNode.sign = ModifierSign.PLUS;
                    if(debug) Log.info("'@' got sign '@'", childNode.getPath(), childNode.sign);
                }
            }

            // patchNode('array': {}) -> modify(override)
//            if(objectNode != null && ClassHelper.isArrayLike(objectNode.type)){
//                if(debug) Log.info("'@' got sign '@'", childNode.getPath(), childNode.sign);
//            }

            parseJson(childObj, childNode, childValue);
        }
    }

    public static JsonValue toJson(PatchNode patchNode){
        return toJson(patchNode, new JsonValue(patchNode.type));
    }

    private static JsonValue toJson(PatchNode patchNode, JsonValue value){
        value.setName(patchNode.key);
        if(patchNode.value != null) value.set(patchNode.value);

        for(PatchNode childNode : patchNode.children.values()){
            JsonValue childValue = new JsonValue(childNode.type);

            value.addChild(childNode.key, childValue);
            toJson(childNode, childValue);
        }

        return value;
    }

    public static JsonValue toPatchJson(PatchNode patchNode){
        return processJson(toJson(patchNode));
    }

    private static JsonValue processJson(JsonValue value){
        Iterator<JsonValue> iterator = value.iterator();

        JsonValue plusValue = null;

        while(iterator.hasNext()){
            JsonValue childValue = iterator.next();
            processJson(childValue);

            if(childValue.name != null && childValue.name.startsWith(appendPrefix)){
                if(plusValue == null){
                    plusValue = new JsonValue(ValueType.array);
                    plusValue.setName(value.name + NodeManager.pathComp + ModifierSign.PLUS.sign);
                    value.parent.addChild(plusValue.name, plusValue);
                    removeJsonValue(value);
                }

                plusValue.addChild(childValue.name, childValue);
            }
        }

        return value;
    }

    private static void extractDotSyntax(JsonValue value){
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

        for(JsonValue childValue : value){
            extractDotSyntax(childValue);
        }
    }

    private static void desugarJson(ObjectNode objectNode, JsonValue value){
        if(objectNode != null){
            if(value.isValue()){
                desugarJson(value, objectNode.type);
            }else if(value.isArray() && objectNode.elementType != null){
                ObjectNode childObj = ObjectResolver.getTemplate(objectNode.elementType);
                for(JsonValue childValue : value){
                    desugarJson(childObj, childValue);
                }
            }
        }

        if(value.isValue()) return;

        for(JsonValue childValue : value){
            ObjectNode childNode = childValue.name == null ||objectNode == null ? null : objectNode.getOrResolve(childValue.name);
            desugarJson(childNode, childValue);
        }
    }

    private static void desugarJson(JsonValue value, Class<?> type){
        // TODO: More sugar syntaxes support
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

        if(value.isObject()){
            // duck like
            if(value.has("item") && value.has("amount")){
                value.set(value.get("item").asString() + "/" + value.get("amount").asString());
            }else if(value.has("liquid") && value.has("amount")){
                value.set(value.get("liquid").asString() + "/" + value.get("amount").asString());
            }
        }

        for(JsonValue childValue : value){
            simplifyPatch(childValue);
        }
        return value;
    }

    private static boolean dotSimplifiable(JsonValue singleEnd){
        return !(singleEnd.isArray() || singleEnd.has("type") || singleEnd.name.equals("consumes"));
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