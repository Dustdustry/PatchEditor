package MinRi2.PatchEditor.export;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.node.*;
import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.part.*;
import mindustry.entities.pattern.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.type.ammo.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;

public class ObjectExporter{
    private static final ObjectMap<Class<?>, ObjectNode> templateMap = new ObjectMap<>();
    private static final ObjectMap<Class<?>, Object> exampleMap = new ObjectMap<>();
    private static final ObjectMap<Class<?>, Seq<String>> fieldBlacklist = ObjectMap.of(
    Block.class, Seq.with("teamRegion", "teamRegions", "consumes"),
    UnitType.class, Seq.with("sample", "aiController", "controller")
    );

    public static JsonValue exportJson(ObjectNode objectNode){
        return exportJson(objectNode, new ExportConfig());
    }

    public static @Nullable JsonValue exportJson(ObjectNode objectNode, ExportConfig config){
        Object object = objectNode.object;
        Class<?> type = objectNode.type;

        if(object instanceof MapEntry<?,?> entry){
            object = entry.value;
            type = objectNode.elementType;
        }

        if(object == null) return null;

        JsonValue result = new JsonValue(ValueType.object);
        if(type.isPrimitive() || Reflect.isWrapper(type)){
            exportPrimitive(object, type, result);
        }else if(type == String.class){
            result.set(String.valueOf(object));
        }else if(object instanceof Color color){
            result.set("#" + color);
        }else if(object instanceof ItemStack stack){
            result.set(stack.item.name + "/" + stack.amount);
        }else if(object instanceof LiquidStack stack){
            result.set(stack.liquid.name + "/" + stack.amount);
        }else if(object instanceof PayloadStack stack){
            result.set(stack.item.name + "/" + stack.amount);
        }else if(object instanceof Vec3 vec){
            result.setType(ValueType.array);
            result.addChild(new JsonValue(vec.x));
            result.addChild(new JsonValue(vec.y));
            result.addChild(new JsonValue(vec.z));
        }else if(object instanceof Vec2 vec){
            result.setType(ValueType.array);
            result.addChild(new JsonValue(vec.x));
            result.addChild(new JsonValue(vec.y));
        }else if(object instanceof Rect rect){
            result.setType(ValueType.array);
            result.addChild(new JsonValue(rect.x));
            result.addChild(new JsonValue(rect.y));
            result.addChild(new JsonValue(rect.width));
            result.addChild(new JsonValue(rect.height));
        }else if(object instanceof Team team){
            if(team.id < Team.baseTeams.length){
                result.set(team.name);
            }else{
                result.set(team.id, null);
            }
        }else if(object instanceof Effect effect){
            String fieldName = findStaticFieldName(effect, Effect.class);
            if(fieldName != null){
                result.set(fieldName);
            }else{
                return null;
            }
        }else if(object instanceof BulletType bullet){
            String fieldName = findStaticFieldName(bullet, BulletType.class);
            if(fieldName != null){
                result.set(fieldName);
            }else{
                exportObject(objectNode, result, config);
            }
        }else if(object instanceof AmmoType ammo){
            if(ammo instanceof ItemAmmoType itemAmmo){
                result.set(itemAmmo.item.name);
            }else if(ammo instanceof PowerAmmoType powerAmmo){
                result.set(powerAmmo.totalPower, null);
            }else {
                exportObject(objectNode, result, config);
            }
        }else if(object instanceof DrawBlock){
            exportObject(objectNode, result, config);
        }else if(object instanceof TextureRegion region){
            if(region == Core.atlas.find("error")) return null;
            String regionName = Core.atlas.getRegionMap().findKey(region, true);
            if(regionName != null){
                result.set(regionName);
            }else{
                return null;
            }
        }else if(object instanceof Sound sound){
            String fieldName = findStaticFieldName(sound, Sound.class);
            if(fieldName != null){
                result.set(fieldName);
            }else{
                return null;
            }
        }else if(object instanceof Music music){
            String fieldName = findStaticFieldName(music, Music.class);
            if(fieldName != null){
                result.set(fieldName);
            }else{
                return null;
            }
        }else if(object instanceof Interp interp){
            String fieldName = findStaticFieldName(interp, Interp.class);
            if(fieldName != null){
                result.set(fieldName);
            }else{
                return null;
            }
        }else if(object instanceof Attribute attr){
            result.set(attr.name);
        }else if(object instanceof ShootPattern){
            exportObject(objectNode, result, config);
        }else if(object instanceof DrawPart){
            exportObject(objectNode, result, config);
        }else if(object instanceof Ability){
            exportObject(objectNode, result, config);
        }else if(object instanceof Weapon){
            exportObject(objectNode, result, config);
        }else if(object instanceof Consume){
            exportObject(objectNode, result, config);
        }else if(object instanceof Enum<?> e){
            result.set(e.name());
        }else if(object instanceof MappableContent mc && objectNode.hasSign(ModifierSign.MODIFY)){
            result.set(mc.name);
        }else if(object instanceof Attributes attributes){
            for(Attribute attr : Attribute.all){
                if(attributes.get(attr) != 0f){
                    result.addChild(attr.name, new JsonValue(attributes.get(attr)));
                }
            }
        }else if(ClassHelper.isContainer(type)){
            result.setType(ClassHelper.isArrayLike(type) ? ValueType.array : ValueType.object);
            for(Entry<String, ObjectNode> entry : objectNode.getChildren()){
                if(!entry.value.isSign()){
                    JsonValue value = exportJson(entry.value, config);
                    if(value != null) result.addChild(entry.key, value);
                }
            }
        }else if(PatchJsonIO.keyFieldsClasses.containsKey(type)){
            result.set(PatchJsonIO.getKeyName(object));
        }else{
            exportFields(objectNode, result, config);
        }

        return result;
    }

    private static boolean shouldExportField(Object value, Object defaultValue, ExportConfig config){
        if(value == null) return config.exportNulls;
        return !equals(value, defaultValue, ClassHelper.unoymousClass(value.getClass()));
    }

    private static void exportFields(ObjectNode objectNode, JsonValue value, ExportConfig config){
        Class<?> actualType = ClassHelper.unoymousClass(objectNode.object.getClass());
        ObjectNode template = getTemplate(actualType);
        Seq<String> blackList = findFieldBlacklist(actualType);

        for(Entry<String, ObjectNode> entry : objectNode.getChildren()){
            ObjectNode childNode = entry.value;
            if(childNode.isSign() || (blackList != null && blackList.contains(entry.key))) continue;
            if(template.object != null){
                ObjectNode templateChild = template.getOrResolve(entry.key);
                if(templateChild != null && !shouldExportField(childNode.object, templateChild.object, config)) continue;
            }

            JsonValue childValue = exportJson(childNode, config);
            if(childValue != null) value.addChild(entry.key, childValue);
        }
    }

    private static void exportPrimitive(Object value, Class<?> type, JsonValue result){
        if(type == float.class || type == Float.class){
            result.set(value == null ? 0f : (Float)value, null);
        }else if(type == double.class || type == Double.class){
            result.set(value == null ? 0.0D : (Double)value, null);
        }else if(type == int.class || type == Integer.class){
            result.set(value == null ? 0 : (Integer)value, null);
        }else if(type == long.class || type == Long.class){
            result.set(value == null ? 0L : (Long)value, null);
        }else if(type == boolean.class || type == Boolean.class){
            result.set(value != null && (Boolean)value);
        }else if(type == short.class || type == Short.class){
            result.set(value == null ? 0 : (Short)value, null);
        }else if(type == byte.class || type == Byte.class){
            result.set(value == null ? 0 : (Byte)value, null);
        }
    }

    public static JsonValue exportObject(ObjectNode objectNode, ExportConfig config){
        JsonValue value = new JsonValue(ValueType.object);
        exportObject(objectNode, value, config);
        return value;
    }

    public static void exportObject(ObjectNode objectNode, JsonValue value, ExportConfig config){
        Object object = objectNode.object;
        if(object instanceof MapEntry<?,?> entry) object = entry.value;

        value.setType(ValueType.object);
        Class<?> type = ClassHelper.unoymousClass(object.getClass());
        String typeName = PatchJsonIO.getClassTypeName(type);
        value.addChild("type", new JsonValue(typeName));
        exportFields(objectNode, value, config);
    }

    private static <T> String findStaticFieldName(Object value, Class<T> sourceClass){
        ObjectMap<String, T> objectNameMap = PatchJsonIO.getKeyEntryMap(sourceClass);
        if(objectNameMap == null) return null;
        return objectNameMap.findKey(value, true);
    }

    public static boolean equals(Object value, Object defaultValue, Class<?> type){
        if(value == null && defaultValue == null) return true;
        if(value == null || defaultValue == null) return false;

        if(ClassHelper.isArray(type)){
            int actualLength = Array.getLength(value);
            int defaultLength = Array.getLength(defaultValue);
            if(actualLength != defaultLength) return false;
            for(int i = 0; i < actualLength; i++){
                if(Array.get(value, i) != Array.get(defaultValue, i)) return false;
            }
            return true;
        }

        return value.equals(defaultValue);
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

    // template with instance example
    private static ObjectNode getTemplate(Class<?> type){
        ObjectNode objectNode = templateMap.get(type);
        if(objectNode != null) return objectNode;

        objectNode = new ObjectNode("", getExample(PatchJsonIO.resolveType(type), type), type);
        templateMap.put(type, objectNode);
        return objectNode;
    }

    // instance example
    private static Object getExample(Class<?> base, Class<?> type){
        if(type == float.class || type == Float.class) return 0f;
        if(type == double.class || type == Double.class) return 0d;
        if(type == boolean.class || type == Boolean.class) return false;
        if(type == short.class || type == Short.class) return (short)0;
        if(type == byte.class || type == Byte.class) return (byte)0;
        if(type == char.class || type == Character.class) return '\0';
        if(type.isArray()) return Reflect.newArray(type.getComponentType(), 0);

        type = PatchJsonIO.resolveType(type);

        Object example = exampleMap.get(type);
        if(example != null) return example;

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

        exampleMap.put(type, example);
        return example;
    }

    public static class ExportConfig{
        public boolean exportNulls = false;
    }
}
