package MinRi2.ContentsEditor.node.modifier;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.node.modifier.equal.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.abilities.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;

import java.lang.reflect.*;

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
        new ModifierConfig(StringModifier.class, StringModifier::new, String.class),

        new ModifierConfig(NumberModifier.class, NumberModifier::new,
        Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
        byte.class, short.class, int.class, long.class, float.class, double.class),

        new ModifierConfig(BooleanModifier.class, BooleanModifier::new, Boolean.class, boolean.class),

        new ModifierConfig(ContentTypeModifier.class, ContentTypeModifier::new,
        Block.class, Item.class, Liquid.class, StatusEffect.class, UnitType.class)
        );

        defaultClassMap.putAll(
        Ability.class, ForceFieldAbility.class
        );
    }

    public static DataModifier<?> getModifier(NodeData node){
        if(canModify(node)){
            Class<?> type = PatchJsonIO.getType(node);
            for(ModifierConfig config : modifyConfig){
                if(config.canModify(type)) return config.getModifier(node);
            }
        }
        return null;
    }

    public static int getModifierIndex(NodeData node){
        if(canModify(node)){
            int i = 0;
            Class<?> type = PatchJsonIO.getType(node);
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
        if(object instanceof MappableContent mc){
            node.getJsonData().set(PatchJsonIO.getKeyName(mc));
        }
    }

    public static NodeData changeType(NodeData node, Class<?> newType){
        Class<?> typeMeta = PatchJsonIO.getTypeMeta(node);
        if(typeMeta == null){
            throw new RuntimeException("Couldn't change " + node.name + "'s type due to the null type of meta.");
        }

        if(!typeMeta.isAssignableFrom(newType)){
            throw new RuntimeException("Couldn't change " + node.name + "'s type '" + typeMeta.getName() + "' due to unassignable type '" + newType.getName() + "'.");
        }
        JsonValue value = PatchJsonIO.toJson(node);
        value.remove("type");

        NodeData childData = NodeModifier.addDynamicChild(node.parentData, newType);
        PatchJsonIO.parseJson(childData, value);
        node.clearJson();

        return childData;
    }

    public static NodeData addDynamicChild(NodeData node){
        return addDynamicChild(node, null);
    }

    public static NodeData addDynamicChild(NodeData node, @Nullable Class<?> type){
        if(!(PatchJsonIO.isArray(node) || PatchJsonIO.isMap(node))) return null;

        Object object = node.getObject();
        if(node.isSign()) object = node.parentData.getObject();

        int nextIndex = -1;
        if(object instanceof Object[] arr){
            nextIndex = arr.length;
        }else if(object instanceof Seq<?> seq){
            nextIndex = seq.size;
        }else if(object instanceof ObjectSet<?> set){
            nextIndex = set.size;
        }

        FieldData meta = node.meta;
        Class<?> actualElemType = type != null ? type : meta.elementType;
        if(nextIndex != -1){
            int index = nextIndex + node.getChildren().size;
            Object example = getExample(actualElemType);
            if(example == null) return null;
            NodeData childData = node.addChild("" + index, example, new FieldData(meta.elementType));
            childData.initJsonData();
            childData.addChild(ModifierSign.MODIFY.sign, new FieldData(example.getClass()));
            handleDynamicData(childData);
            return childData;
        }

        if(object instanceof ObjectMap<?,?>){
            String name = "<key>";
            if(MappableContent.class.isAssignableFrom(meta.keyType)){
                ContentType contentType = PatchJsonIO.getContentType(meta.keyType);
                if(contentType != null){
                    name = PatchJsonIO.getKeyName(Vars.content.getBy(contentType).first());
                }
            }

            Object example = getExample(actualElemType);
            if(example == null) return null;
            NodeData childData = node.addChild(name, example, new FieldData(meta.elementType, meta.elementType, meta.keyType));
            childData.initJsonData();
            childData.addChild(ModifierSign.MODIFY.sign, new FieldData(example.getClass()));
            handleDynamicData(childData);
            return childData;
        }

        return null;
    }

    public static Object getExample(Class<?> type){
        if(type.isArray()) return Reflect.newArray(type.getComponentType(), 0);

        type = handleType(type);
        if(type == null) return null;

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
                example = PatchJsonIO.getParser().getJson().fromJson(type, "{}");
            }catch(Exception ignored){
                return null;
            }
        }

        exampleMap.put(type, example);
        return example;
    }

    public static class ModifierConfig{
        public final Seq<Class<?>> modifierTypes = new Seq<>();
        private final Pool<DataModifier<?>> pool;

        @SuppressWarnings("unchecked")
        public ModifierConfig(Class<? extends DataModifier<?>> clazz, Prov<? extends DataModifier<?>> prov, Class<?>... types){
            pool = Pools.get((Class)clazz, prov);
            modifierTypes.addAll(types);
        }

        public boolean canModify(Class<?> type){
            return modifierTypes.contains(c -> c.isAssignableFrom(type));
        }

        public DataModifier<?> getModifier(NodeData nodeData){
            DataModifier<?> modifier = pool.obtain();
            modifier.setNodeData(nodeData);
            return modifier;
        }
    }
}
