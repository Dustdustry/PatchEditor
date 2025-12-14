package MinRi2.ContentsEditor.node.modifier;

import MinRi2.ContentsEditor.node.*;
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
import mindustry.world.blocks.payloads.*;

import java.lang.reflect.*;

import static MinRi2.ContentsEditor.node.modifier.EqualModifier.*;

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

    public static DataModifier<?> getModifier(NodeData node){
        if(canModify(node)){
            Class<?> type = PatchJsonIO.getTypeIn(node);
            for(ModifierConfig config : modifyConfig){
                if(config.canModify(type)) return config.getModifier(node);
            }
        }
        return null;
    }

    public static int getModifierIndex(NodeData node){
        if(canModify(node)){
            int i = 0;
            Class<?> type = PatchJsonIO.getTypeIn(node);
            for(ModifierConfig config : modifyConfig){
                if(config.canModify(type)) return i;
                i++;
            }
        }
        return -1;
    }

    public static boolean canModify(NodeData node){
        return node.getSign(ModifierSign.MODIFY) != null;
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

    private static void handleDynamicData(NodeData node){
        Object object = node.getObject();

        JsonValue value = new JsonValue("");

        // set default value
        if(object instanceof MappableContent mc){
            value.set(PatchJsonIO.getKeyName(mc));
        }

        if(object instanceof ItemStack){
            ItemStack stack = (ItemStack)getExample(null, ItemStack.class);
            if(stack == null) return;
            value.set(PatchJsonIO.getKeyName(stack.item) + "/" + 0);
        }else if(object instanceof PayloadStack){
            PayloadStack stack = (PayloadStack)getExample(null, PayloadStack.class);
            if(stack == null) return;
            value.set(PatchJsonIO.getKeyName(stack.item) + "/" + 0);
        }else if(object instanceof LiquidStack){
            LiquidStack stack = (LiquidStack)getExample(null, LiquidStack.class);
            if(stack == null) return;
            value.set(PatchJsonIO.getKeyName(stack.liquid) + "/" + 0);
        }

        if(value.isString() && value.asString().isEmpty()) return;

        PatchJsonIO.parseJson(node, value);
    }

    public static NodeData changeType(NodeData node, Class<?> newType){
        Class<?> typeMeta = PatchJsonIO.getTypeIn(node);
        if(typeMeta == null){
            throw new RuntimeException("Couldn't change " + node.name + "'s type due to the null type of meta.");
        }

        if(!typeMeta.isAssignableFrom(newType)){
            throw new RuntimeException("Couldn't change the type of'" + node.name + "' to '" + typeMeta.getName() + "' due to unassignable type '" + newType.getName() + "'.");
        }

        Object example = getExample(typeMeta, newType);
        if(example == null) return null;

        JsonValue value = PatchJsonIO.toJson(node);
        value.remove("type");

        // remove old
        NodeData parent = node.parentData;
        node.clearJson();
        node.remove();

        NodeData newData = parent.addChild(node.name, example, new FieldData(node.meta.type, node.meta.elementType, node.meta.keyType));
        newData.initJsonData();
        PatchJsonIO.parseJson(newData, value);
        return newData;
    }

    public static NodeData addDynamicChild(NodeData node){
        return addDynamicChild(node, null, null);
    }

    public static NodeData addDynamicChild(NodeData node, @Nullable Class<?> type){
        return addDynamicChild(node, type, null);
    }

    public static NodeData addDynamicChild(NodeData node, @Nullable Class<?> type, @Nullable String keyName){
        NodeData checkNode = node.isSign() ? node.parentData : node;
        if(!(PatchJsonIO.isArrayLike(checkNode) || PatchJsonIO.isMap(checkNode))) return null;

        Object object = node.getObject();
        if(node.isSign()) object = node.parentData.getObject();

        FieldData meta = node.meta;
        Class<?> baseType = meta.elementType;
        Class<?> actualElemType = type != null ? type : baseType;

        int nextIndex = -1;
        if(object instanceof Object[] arr){
            nextIndex = arr.length;
        }else if(object instanceof Seq<?> seq){
            nextIndex = seq.size;
        }else if(object instanceof ObjectSet<?> set){
            nextIndex = set.size;
        }

        if(nextIndex != -1){
            int index = nextIndex + node.getChildren().size;
            Object example = getExample(baseType, actualElemType);
            if(example == null) return null;
            NodeData childData = node.addChild("" + index, example, new FieldData(baseType));
            childData.initJsonData();
            childData.addChild(ModifierSign.MODIFY.sign, new FieldData(example.getClass()));
            handleDynamicData(childData);
            return childData;
        }

        if(object instanceof ObjectMap<?,?>){
            String name = keyName == null ? "<key>" : keyName;

            Object example = getExample(baseType, actualElemType);
            if(example == null) return null;
            NodeData childData = node.addChild(name, example, new FieldData(baseType, baseType, meta.keyType));
            childData.initJsonData();
            childData.addChild(ModifierSign.MODIFY.sign, new FieldData(example.getClass()));
            handleDynamicData(childData);
            return childData;
        }

        return null;
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

        public DataModifier<?> getModifier(NodeData nodeData){
            DataModifier<?> modifier = prov.get();
            modifier.setNodeData(nodeData);
            return modifier;
        }
    }
}
