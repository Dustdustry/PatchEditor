package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.node.PatchJsonTransform.*;
import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.ui.editor.EditorSettings.*;
import arc.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.JsonWriter.*;
import arc.util.serialization.Jval.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;

public class PatchJsonIO{
    public static final boolean debug = false;

    public static final int simplifySingleCount = 3;

    private static ContentParser parser;
    public static final String appendPrefix = "#ADD_";

    private static ObjectMap<String, ContentType> nameToType;
    public static ObjectMap<Class<?>, ContentType> classContentType;

    private static final ObjectMap<Class<?>, ObjectMap<String, Object>> objectNameMap = ObjectMap.of(
    TextureRegion.class, Core.atlas.getRegions()
    );

    public static final ObjectMap<Class<?>, Class<?>> defaultClassMap = ObjectMap.of(
        Ability.class, ForceFieldAbility.class
    );
    public static final Seq<Class<?>> fixedTypeClasses = Seq.with(
    UnlockableContent.class,
    ItemStack.class, LiquidStack.class, PayloadStack.class
    );
    public static final ObjectMap<Class<?>, Class<?>> keyFieldsClasses = ObjectMap.of(
    Effect.class, Fx.class,
    BlockFlag.class, BlockFlag.class,
    BuildVisibility.class, BuildVisibility.class,
    Interp.class, Interp.class,
    Sound.class, Sounds.class,
    BulletType.class, Bullets.class
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
            String buildIn = getKeyEntryMap(type).findKey(object, true);
            if(buildIn != null) return buildIn;
        }

        return String.valueOf(object);
    }

    public static <T> ObjectMap<String, T> getKeyEntryMap(Class<T> type){
        return getKeyEntryMap(type, keyFieldsClasses.get(type));
    }

    @SuppressWarnings("unchecked")
    public static <T> ObjectMap<String, T> getKeyEntryMap(Class<T> type, Class<?> declare){
        if(declare == null) return null;

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
            if(type == int.class || type == Integer.class) return Integer.parseInt(patchNode.value);
            if(type == short.class || type == Short.class) return Short.parseShort(patchNode.value);
            if(type == byte.class || type == Byte.class) return Byte.parseByte(patchNode.value);

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
        PatchJsonTransform.extractDotSyntax(value);
        PatchJsonTransform.desugarJson(objectNode, value);
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

    public static String toPatch(ObjectNode objectNode, JsonValue value){
        // sugar
        SugarJsonConfig sugarJsonConfig = new SugarJsonConfig()
        .sugarStacks(Core.settings.getBool("patch-editor.sugar.stacks"));

        PatchJsonTransform.sugarPatch(objectNode, value, sugarJsonConfig);

        // process
        PatchJsonTransform.processJson(objectNode, value);

        // simplify
        if(Core.settings.getBool("patch-editor.simplifyPath")){
            PatchJsonTransform.simplifyPath(value);
        }

        // to string
        String exportType = Core.settings.getString("patch-editor.exportType");
        if(ExportType.hjson.is(exportType)){
            return Jval.read(value.toJson(OutputType.json)).toString(Jformat.hjson);
        }else{
            return value.toJson(OutputType.json);
        }
    }

    public static String toPatch(ObjectNode objectNode, PatchNode patchNode){
        return toPatch(objectNode, toJson(patchNode));
    }
}
