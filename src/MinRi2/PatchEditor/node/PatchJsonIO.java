package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.node.patch.*;
import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.Jval.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;
import java.util.*;

public class PatchJsonIO{
    public static final boolean debug = false;

    public static final int simplifySingleCount = 3;

    private static ContentParser parser;
    public static final String appendPrefix = "#ADD_";

    private static ObjectMap<String, ContentType> nameToType;
    public static ObjectMap<Class<?>, ContentType> classContentType;

    private static final ObjectMap<Class<?>, ObjectMap<String, Object>> objectNameMap = new ObjectMap<>();

    public static final ObjectMap<Class<?>, Class<?>> defaultClassMap = ObjectMap.of(
        Ability.class, ForceFieldAbility.class
    );
    public static final Seq<Class<?>> fixedTypeClasses = Seq.with(
    UnitType.class, UnlockableContent.class,
    ItemStack.class, LiquidStack.class, PayloadStack.class
    );
    public static final ObjectMap<Class<?>, Class<?>> keyFieldsClasses = ObjectMap.of(
    Effect.class, Fx.class,
    BlockFlag.class, BlockFlag.class,
    BuildVisibility.class, BuildVisibility.class,
    Interp.class, Interp.class
    );

    // internal key name
    public static String getKeyName(Object object){
        if(object == null) return "null";
        if(object instanceof MappableContent mc) return mc.name;
        if(object instanceof Enum<?> e) return e.name();
        if(object instanceof Class<?> clazz) return clazz.getName();
        if(object instanceof TextureRegion region){
            String key = Core.atlas.getRegionMap().findKey(region, true);
            return key == null ? "error" : key;
        }

        Class<?> type = keyFieldsClasses.keys().toSeq().find(c -> c.isAssignableFrom(object.getClass()));
        if(type != null){
            String buildIn = getKeyEntryMap(type, keyFieldsClasses.get(type)).findKey(object, true);
            if(buildIn != null) return buildIn;
        }

        return String.valueOf(object);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectMap<String, T> getKeyEntryMap(Class<T> type, Class<?> declare){
        ObjectMap<String, Object> map = objectNameMap.get(declare);
        if(map != null) return (ObjectMap<String, T>)map;

        map = Seq.select(declare.getFields(), f -> f.getType() == type).asMap(Field::getName, Reflect::get);
        objectNameMap.put(declare, map);
        return (ObjectMap<String, T>)map;
    }

    // internal type name
    public static String getClassTypeName(Class<?> clazz){
        String key = ClassMap.classes.findKey(clazz, true);
        return key != null ? key : clazz.getName();
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

    public static ContentType classContentType(Class<?> type){
        if(classContentType == null){
            classContentType = new ObjectMap<>();
            for(ContentType contentType : ContentType.all){
                if(contentType.contentClass != null){
                    classContentType.put(contentType.contentClass, contentType);
                }
            }
        }

        return classContentType.get(type);
    }

    public static boolean overrideable(Class<?> type){
        return !(type.isPrimitive() || type == String.class // String is regarded as primitive type in json
        || Reflect.isWrapper(type) || ClassHelper.isMap(type)); // map is unable to override
    }

    public static boolean typeOverrideable(Class<?> type){
        return overrideable(type) && !(ClassHelper.isArrayLike(type) || fixedTypeClasses.contains(c -> c.isAssignableFrom(type)));
    }

    public static Class<?> resolveType(@Nullable String typeJson){
        return typeJson != null && ClassMap.classes.containsKey(typeJson) ? resolveType(ClassMap.classes.get(typeJson)) : null;
    }

    public static Class<?> resolveType(Class<?> type){
        if(type.isPrimitive() || ClassHelper.isArray(type)) return type;

        int typeModifiers = type.getModifiers();
        if(!Modifier.isAbstract(typeModifiers) && !Modifier.isInterface(typeModifiers)) return type;

        Class<?> defaultType = defaultClassMap.get(type);
        if(defaultType != null) return defaultType;

        return type;
    }

    /** Get available type in ContentParser#classParsers */
    public static Class<?> getTypeParser(Class<?> type){
        if(type.isPrimitive()) return type;

        Class<?> toppest = type;

        Class<?> current = type;
        while(current != Object.class){
            if(ClassMap.classes.findKey(current, true) != null) toppest = current;
            current = current.getSuperclass();
        }

        return toppest;
    }

    public static int getContainerSize(Object containerLike){
        if(containerLike instanceof Object[] arr) return arr.length;
        if(containerLike instanceof Seq<?> seq) return seq.size;
        if(containerLike instanceof ObjectSet<?> set) return set.size;
        if(containerLike instanceof ObjectMap<?,?> map) return map.size;
        if(containerLike instanceof ObjectFloatMap<?> map) return map.size;
        return -1;
    }

    // For stimulating patched result.
    public static Object cloneObject(Object object){
        if(object instanceof ItemStack stack) return new ItemStack(stack.item, stack.amount);
        if(object instanceof LiquidStack stack) return new LiquidStack(stack.liquid, stack.amount);
        if(object instanceof PayloadStack stack) return new PayloadStack(stack.item, stack.amount);
        return null;
    }

    public static Object parseJsonObject(PatchNode patchNode, ObjectNode objectNode, Object original){
        Json json = PatchJsonIO.getParser().getJson();
        try{
            Class<?> type = objectNode.type;
            if(type == float.class || type == Float.class) return Float.parseFloat(patchNode.value);
            if(type == double.class || type == Double.class) return Double.parseDouble(patchNode.value);
            if(type == long.class || type == Long.class) return Long.parseLong(patchNode.value);
            if(type == int.class || type == Integer.class
            || type == short.class || type == Short.class
            || type == byte.class || type == Byte.class) return Integer.parseInt(patchNode.value);

            JsonValue value = PatchJsonIO.toJson(patchNode);
            if(patchNode.value != null) return json.readValue(type, objectNode.elementType, value);

            Object copied = PatchJsonIO.cloneObject(original);
            if(copied == null) return json.readValue(type, objectNode.elementType, value);

            // stimulate patch applying
            json.readFields(copied, value);
            return copied;
        }catch(Exception e){
            // may expect class value
            if(patchNode.value != null){
                Class<?> type = PatchJsonIO.resolveType(patchNode.value);
                if(type != null) return type;
            }
            return original;
        }
    }

    public static void parseJson(ObjectNode objectNode, PatchNode patchNode, String patch){
        JsonValue value = getParser().getJson().fromJson(null, Jval.read(patch).toString(Jformat.plain));
        extractDotSyntax(value);
        desugarJson(objectNode, value);
        parseJson(objectNode, patchNode, value);
    }

    public static void parseJson(ObjectNode objectNode, PatchNode patchNode, JsonValue value){
        // sign is seen as attribute in PatchNode, not a node
        if(!value.isValue() && value.has(ModifierSign.PLUS.sign)){
            JsonValue plusValue = value.remove(ModifierSign.PLUS.sign);

            int i = 0;
            if(plusValue.isArray()){
                // patchNode('+': [{}, ""]) -> multiple append
                for(JsonValue childValue : plusValue){
                    PatchNode childNode = patchNode.getOrCreate(appendPrefix + i++);
                    if(debug) Log.info("'@' got sign @", childNode.getPath(), childNode.sign);
                    parseJson(null, childNode, childValue);
                    childNode.sign = ModifierSign.PLUS;
                }
            }else if(plusValue.isObject()){
                // patchNode('+': {}) -> single append
                PatchNode childNode = patchNode.getOrCreate(appendPrefix + i);
                childNode.sign = ModifierSign.PLUS;
                if(debug) Log.info("'@' got sign @", childNode.getPath(), childNode.sign);
                parseJson(null, childNode, plusValue);
            }

            if(value.child == null){
                JsonHelper.remove(value);
                return;
            }
        }

        if(value.isArray()){
            // patchNode('array': []) -> override array.
            int i = 0;
            for(JsonValue childValue : value){
                PatchNode childNode = patchNode.getOrCreate("" + i++);
                childNode.sign = ModifierSign.PLUS;
                parseJson(null, childNode, childValue);
            }
            patchNode.type = ValueType.array;
            patchNode.sign = ModifierSign.MODIFY;
            return;
        }

        if(value.isValue()) patchNode.value = value.asString();
        patchNode.type = value.type();

        if(value.isValue()) return;

        for(JsonValue childValue : value){
            String name = childValue.name;

            ObjectNode childObj = objectNode == null ? null : objectNode.getOrResolve(name);
            PatchNode childNode = patchNode.getOrCreate(name);

            // map sign assign
            if(objectNode != null && ClassHelper.isMap(objectNode.type)){
                if(childValue.isValue() && ModifierSign.REMOVE.sign.equals(childValue.asString())){
                    // patchNode('map': {xxx: '-'}) -> remove the key
                    childNode.sign = ModifierSign.REMOVE;
                }else{
                    // patchNode('map': {}) -> modify(override) or append key
                    Object key = parser.getJson().readValue(objectNode.keyType, new JsonValue(childValue.name));
                    if(key != null && (objectNode.object instanceof ObjectMap objectMap && !objectMap.containsKey(key))
                    || (objectNode.object instanceof ObjectFloatMap floatMap && !floatMap.containsKey(key))){
                        childNode.sign = ModifierSign.PLUS;
                        if(debug) Log.info("'@' got sign '@'", childNode.getPath(), childNode.sign);
                    }
                }
            }

            // override sign assign
            if(childObj != null && (overrideable(childObj.type) && childObj.object == null || typeOverrideable(childObj.type) && childValue.has("type"))){
                childNode.sign = ModifierSign.MODIFY;
                if(debug) Log.info("'@' got sign '@'", childNode.getPath(), childNode.sign);
            }

            // patchNode('array': {}) -> normal modify(override) do nothing
            parseJson(childObj, childNode, childValue);
        }
    }

    /** patchTree to jsonTree */
    public static JsonValue toJson(PatchNode patchNode){
        return toJson(patchNode, new JsonValue(patchNode.type));
    }

    private static JsonValue toJson(PatchNode patchNode, JsonValue value){
        value.setName(patchNode.key);
        if(patchNode.value != null){
            if(patchNode.type == ValueType.doubleValue || patchNode.type == ValueType.longValue){
                value.set(Strings.parseDouble(patchNode.value, 0), patchNode.value);
            }else{
                value.set(patchNode.value);
            }
        }

        JsonValue appendValue = null;
        for(PatchNode childNode : patchNode.children.values()){
            JsonValue childValue = new JsonValue(childNode.type);

            if(childNode.key.startsWith(appendPrefix)){
                if(appendValue == null){
                    appendValue = new JsonValue(ValueType.array);
                    value.addChild(ModifierSign.PLUS.sign, appendValue);
                }

                appendValue.addChild(childValue.name, childValue);
            }else{
                value.addChild(childNode.key, childValue);
            }

            toJson(childNode, childValue);
        }

        return value;
    }

    /** jsonTree to patchJsonTree. */
    public static JsonValue processJson(ObjectNode objectNode, JsonValue value){
        if(value.isValue()) return value;

        for(JsonValue child : value){
            ObjectNode childNode = child.name != null ? objectNode.getOrResolve(child.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType);
            if(childNode != null) processJson(childNode, child);
        }

        Seq<JsonValue> result = new Seq<>();
        for(JsonValue childValue : value){
            ObjectNode childNode = childValue.name != null ? objectNode.getOrResolve(childValue.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType);

            if(childNode == null){
                result.add(childValue);
                continue;
            }

            if(childNode.isMultiArrayLike()){
                for(JsonValue indexValue : childValue){
                    JsonHelper.remove(indexValue);
                    indexValue.setName(childValue.name + "." + indexValue.name);
                    result.add(indexValue);
                }
            }else if(childNode.isArrayLike() && childValue.has(ModifierSign.PLUS.sign)){
                JsonValue plusValue = childValue.remove(ModifierSign.PLUS.sign);
                if(childValue.child != null){
                    result.add(childValue);
                }
                plusValue.setName(childValue.name + "." + plusValue.name);
                result.add(plusValue);
            }else if(childNode.type == Consume.class && childNode.name.equals("remove")){
                // remove: {item: -, liquid: -} -> remove: [item, liquid]
                childValue.setType(ValueType.array);
                for(JsonValue removed : childValue){
                    removed.set(removed.name());
                }
                result.insert(0, childValue);
            }else{
                result.add(childValue);
            }
        }

        // mount again
        value.child = result.size > 0 ? result.get(0) : null;
        value.size = result.size;
        JsonValue prev = null;
        for(JsonValue jsonValue : result){
            jsonValue.parent = value;
            jsonValue.prev = prev;
            jsonValue.next = null;
            if(prev != null) prev.next = jsonValue;

            prev = jsonValue;
        }

        return value;
    }

    /** patchTree to patchJsonTree. */
    public static JsonValue toPatchJson(ObjectNode objectNode, PatchNode patchNode){
        return processJson(objectNode, toJson(patchNode));
    }

    private static void extractDotSyntax(JsonValue value){
        if(value.name != null && value.parent != null && value.name.contains(NodeManager.pathComp)){
            String[] names = value.name.split(NodeManager.pathSplitter);

            int i = 0;
            JsonValue currentParent = new JsonValue(ValueType.object);
            currentParent.setName(names[i++]);
            JsonHelper.replace(value, currentParent); // don't affect the order

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
        boolean isValue = value.isValue();

        if(objectNode != null){
            desugarJson(value, objectNode.type);

            // desugarJson may change the value type so cache it
            if(isValue) return;

            if(ClassHelper.isArrayLike(objectNode.type)){
                // "requirements": ["item/amount"] | {+: [], {"item": "xxx"}}
                ObjectNode childNode = ObjectResolver.getTemplate(objectNode.elementType);
                for(JsonValue childValue : value){
                    if(ModifierSign.PLUS.sign.equals(childValue.name)){
                        ObjectNode plusNode = objectNode.getOrResolve(childValue.name);
                        desugarJson(plusNode, childValue);
                    }else{
                        desugarJson(childNode, childValue);
                    }
                }
                return;
            }
        }

        if(value.isValue()) return;

        for(JsonValue childValue : value){
            ObjectNode childNode = childValue.name == null || objectNode == null ? null : objectNode.getOrResolve(childValue.name);
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
        }else if(type == Consume.class){
            if(value.name.equals("remove")){
                if(value.isString()){
                    // remove: item -> remove: [item]
                    String removed = value.asString();
                    value.setType(ValueType.array);
                    value.addChild("", new JsonValue(removed));
                }else if(value.isArray()){
                    // remove: [item, liquid] -> remove: {item: -, liquid: -}
                    value.setType(ValueType.object);
                    for(JsonValue child : value){
                        if(child.isString()){
                            child.setName(child.asString());
                            child.set(ModifierSign.REMOVE.sign);
                        }
                    }
                }
            }
        }if(type == ConsumeItems.class){
            if(value.isString()){
                // items: copper/2 -> items: {items: [copper/2]}
                String item = value.asString();
                value.setType(ValueType.object);
                JsonValue itemsValue = new JsonValue(ValueType.array);
                value.addChild("items", itemsValue);
                itemsValue.addChild("", new JsonValue(item));
            }else if(value.isArray()){
                // items: [copper/2] -> items: {items: [copper/2]}
                value.setType(ValueType.object);
                JsonValue itemsValue = new JsonValue(ValueType.array);
                JsonHelper.moveChild(value, itemsValue);
                value.addChild("items", itemsValue);
            }
        }else if(type == ConsumeLiquids.class){
            if(value.isArray()){
                // liquids: [water/0.1] -> liquids: {liquids: [water/0.1]}
                value.setType(ValueType.object);
                JsonValue liquidsValue = new JsonValue(ValueType.array);
                JsonHelper.moveChild(value, liquidsValue);
                value.addChild("liquids", liquidsValue);
            }
        }else if(type == ConsumePower.class){
            if(value.isNumber()){
                // power: 10 -> power: {usage: 10}
                float num = value.asFloat();
                value.setType(ValueType.object);
                value.addChild("usage", new JsonValue(num));
            }
        }else if(value.isArray()){
            /* object: [{}] -> object: { type: MultiXXX, objects: [{}]}*/
            if(type == Effect.class){
                /* to MultiEffect */
                value.setType(ValueType.object);
                JsonValue elementValue = new JsonValue(ValueType.array);
                JsonHelper.moveChild(value, elementValue);

                value.addChild("type", new JsonValue(getClassTypeName(MultiEffect.class)));
                value.addChild("effects", elementValue);
            }else if(type == BulletType.class){
                /* to MultiBulletType */
                value.setType(ValueType.object);
                JsonValue elementValue = new JsonValue(ValueType.array);
                JsonHelper.moveChild(value, elementValue);

                value.addChild("type", new JsonValue(getClassTypeName(MultiBulletType.class)));
                value.addChild("bullets", elementValue);
            }else if(type == DrawBlock.class){
                /* to DrawMulti */
                value.setType(ValueType.object);
                JsonValue elementValue = new JsonValue(ValueType.array);
                JsonHelper.moveChild(value, elementValue);

                value.addChild("type", new JsonValue(getClassTypeName(DrawMulti.class)));
                value.addChild("drawers", elementValue);
            }
        }
    }

    public static void simplifyPatch(JsonValue value){
        if(value.parent == null){
            for(JsonValue childValue : value){
                simplifyPatch(childValue);
            }
            return;
        }

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
            JsonHelper.replace(value, singleEnd);
            value = singleEnd;
        }

        if(value.isObject()){
            // duck like
            if(value.has("item") && value.has("amount")){
                value.set(value.get("item").asString() + "/" + value.get("amount").asString());
                return;
            }else if(value.has("liquid") && value.has("amount")){
                value.set(value.get("liquid").asString() + "/" + value.get("amount").asString());
                return;
            }
        }

        for(JsonValue childValue : value){
            simplifyPatch(childValue);
        }
    }

    private static boolean dotSimplifiable(JsonValue singleEnd){
        return !(singleEnd.isArray() || singleEnd.has("type") || singleEnd.name.equals("consumes"));
    }

    public static JsonValue migrateTweaker(String patch){
        JsonValue tweakerJson = getParser().getJson().fromJson(null, Jval.read(patch).toString(Jformat.plain));
        return migrateTweaker(tweakerJson);
    }

    private static JsonValue migrateTweaker(JsonValue json){
        if(json.isValue()) return json;

        for(JsonValue childValue : json){
            if(childValue.name != null){
                if(childValue.name.startsWith("#")){
                    String key = childValue.name.substring(1);
                    childValue.setName(key);

                    if(json.isObject() && Strings.canParsePositiveInt(key)){
                        json.setType(ValueType.array);
                    }
                }else if(childValue.isValue() && childValue.name.equals("=")){
                    json.set(childValue.asString());
                    json.setType(childValue.type());
                    break;
                }
            }

            migrateTweaker(childValue);
        }
        return json;
    }
}