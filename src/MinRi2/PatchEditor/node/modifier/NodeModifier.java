package MinRi2.PatchEditor.node.modifier;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.EditorNode.*;
import arc.func.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.abilities.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;

import java.lang.reflect.*;

import static MinRi2.PatchEditor.node.modifier.EqualModifier.*;

/**
 * @author minri2
 * Create by 2024/2/16
 */
public class NodeModifier{
    public static final Seq<ModifierConfig> modifyConfig = new Seq<>();
    public static final ObjectMap<Class<?>, Object> exampleMap = new ObjectMap<>();

    public static final ObjectMap<Class<?>, Class<?>> defaultClassMap = new ObjectMap<>();

    static {
        modifyConfig.addAll(
        new ModifierConfig(StringModifier::new, String.class),

        new ModifierConfig(NumberModifier::new,
        Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
        byte.class, short.class, int.class, long.class, float.class, double.class),

        new ModifierConfig(BooleanModifier::new, Boolean.class, boolean.class),

        new ModifierConfig(ContentTypeModifier::new,
        Block.class, Item.class, Liquid.class, StatusEffect.class, UnitType.class),

        new ModifierConfig(ColorModifier::new, Color.class)

        );

        defaultClassMap.putAll(
        Ability.class, ForceFieldAbility.class
        );
    }

    public static DataModifier<?> getModifier(EditorNode node){
        if(canModify(node)){
            Class<?> type = node.getTypeIn();
            for(ModifierConfig config : modifyConfig){
                if(config.canModify(type)) return config.getModifier(node);
            }
        }
        return null;
    }

    public static int getModifierIndex(EditorNode node){
        if(canModify(node)){
            int i = 0;
            Class<?> type = node.getTypeIn();
            for(ModifierConfig config : modifyConfig){
                if(config.canModify(type)) return i;
                i++;
            }
        }
        return -1;
    }

    public static boolean canModify(EditorNode node){
        return node.objectNode != null && node.objectNode.hasSign(ModifierSign.MODIFY);
    }

    private static Class<?> handleType(Class<?> type){
        int typeModifiers = type.getModifiers();
        if(!Modifier.isAbstract(typeModifiers) && !Modifier.isInterface(typeModifiers)) return type;

        Class<?> defaultType = defaultClassMap.get(type);
        if(defaultType != null) return defaultType;

        return ClassMap.classes.values().toSeq().find(c -> {
            int mod = c.getModifiers();
            if(Modifier.isAbstract(mod) || Modifier.isInterface(mod)) return false;
            return type.isAssignableFrom(c);
        });
    }

    private static void handleDynamicData(DynamicEditorNode node){
        Object object = node.example;

        JsonValue value = new JsonValue("");

        // set default value
        if(object instanceof MappableContent mc){
            value.set(PatchJsonIO.getKeyName(mc));
        }

        if(object instanceof ItemStack stack){
            value.set(PatchJsonIO.getKeyName(stack.item) + "/" + 0);
        }else if(object instanceof PayloadStack stack){
            value.set(PatchJsonIO.getKeyName(stack.item) + "/" + 0);
        }else if(object instanceof LiquidStack stack){
            value.set(PatchJsonIO.getKeyName(stack.liquid) + "/" + 0);
        }

        if(value.isString() && value.asString().isEmpty()) return;

//        PatchJsonIO.parseJson(node, value);
    }

    private static JsonValue buildExampleValue(Class<?> type){
        String typeName = ClassMap.classes.findKey(type, true);
        if(typeName == null) typeName = type.getName();

        JsonValue value = new JsonValue(ValueType.object);
        value.addChild("type", new JsonValue(typeName));
        return value;
    }

    public static Object getExample(Class<?> base, Class<?> type){
        if(type.isArray()) return Reflect.newArray(type.getComponentType(), 0);

        type = handleType(type);
        if(type == null) return null;
        if(base == null) base = type;

        Object example = exampleMap.get(type);
        if(example != null) return example;

        if(MappableContent.class.isAssignableFrom(type)){
            ContentType contentType = PatchJsonIO.getContentType(type);
            if(contentType != null){
                example = Vars.content.getBy(contentType).first();
            }
        }

        if(example == null){
            try{
                Json parserJson = PatchJsonIO.getParser().getJson();
                // Invoke internalRead to skip null fields checking.
                example = Reflect.invoke(parserJson, "internalRead", new Object[]{base, null, buildExampleValue(type), null}, Class.class, Class.class, JsonValue.class, Class.class);
            }catch(Exception ignored){
                return null;
            }
        }

        exampleMap.put(type, example);
        return example;
    }

    public static class ModifierConfig{
        public final Seq<Class<?>> modifierTypes = new Seq<>();
        private final Prov<DataModifier<?>> prov;

        public ModifierConfig(Prov<DataModifier<?>> prov, Class<?>... types){
            this.prov = prov;
            modifierTypes.addAll(types);
        }

        public boolean canModify(Class<?> type){
            return modifierTypes.contains(c -> c.isAssignableFrom(type));
        }

        public DataModifier<?> getModifier(EditorNode nodeData){
            DataModifier<?> modifier = prov.get();
            modifier.setData(nodeData);
            return modifier;
        }
    }
}
