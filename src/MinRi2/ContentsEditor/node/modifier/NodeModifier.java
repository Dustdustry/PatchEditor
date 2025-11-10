package MinRi2.ContentsEditor.node.modifier;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.node.modifier.equal.*;
import arc.func.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.pooling.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.*;
import mindustry.ctype.*;
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
    }

    public static DataModifier<?> getModifier(NodeData node){
        if(canModify(node)){
            for(ModifierConfig config : modifyConfig){
                if(config.canModify(node)) return config.getModifier(node);
            }
        }
        return null;
    }

    public static int getModifierIndex(NodeData node){
        if(canModify(node)){
            int i = 0;
            for(ModifierConfig config : modifyConfig){
                if(config.canModify(node)) return i;
                i++;
            }
        }
        return -1;
    }

    public static boolean canModify(NodeData node){
        return node.getSign(ModifierSign.MODIFY) != null;
    }

    public static boolean hasCustomChild(NodeData signNode){
        if(!signNode.isSign()) return false;

        NodeData node = signNode.parentData;
        return PatchJsonIO.isArray(node) || PatchJsonIO.isMap(node);
    }

    public static NodeData addCustomChild(NodeData signNode){
        if(!hasCustomChild(signNode)) return null;

        Object object = signNode.parentData.getObject();

        int nextIndex = -1;
        if(object instanceof Object[] arr){
            nextIndex = arr.length;
        }else if(object instanceof Seq<?> seq){
            nextIndex = seq.size;
        }else if(object instanceof ObjectSet<?> set){
            nextIndex = set.size;
        }

        FieldData meta = signNode.meta;
        if(nextIndex != -1){
            int index = nextIndex + signNode.getChildren().size;
            Object example = getExample(meta.elementType);
            if(example == null) return null;
            NodeData childData = signNode.addChild("" + index, example, new FieldData(example.getClass(), null, null));
            childData.initJsonData();
            return childData;
        }

        if(object instanceof ObjectMap<?,?>){
            String name = "<key>";
            if(MappableContent.class.isAssignableFrom(meta.keyType)){
                ContentType contentType = PatchJsonIO.contentClassTypeMap.get(meta.keyType);
                if(contentType != null){
                    name = PatchJsonIO.getKeyName(Vars.content.getBy(contentType).first());
                }
            }

            Object example = getExample(meta.elementType);
            if(example == null) return null;
            NodeData childData = signNode.addChild(name, example, new FieldData(example.getClass(), example.getClass(), meta.keyType));
            childData.initJsonData();
            return childData;
        }

        return null;
    }

    public static Object getExample(Class<?> type){
        Object example = exampleMap.get(type);
        if(example != null) return example;

        for(Entry<String, Class<?>> entry : ClassMap.classes){
            Class<?> clazz = entry.value;
            int modifiers = clazz.getModifiers();
            if(!Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers)
            && type.isAssignableFrom(clazz)){
                type = clazz;
                break;
            }
        }

        try{
            example = type.getConstructor().newInstance();
        }catch(Exception e){
            return null;
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

        public boolean canModify(NodeData node){
            return modifierTypes.contains(PatchJsonIO.getType(node));
        }

        public DataModifier<?> getModifier(NodeData nodeData){
            DataModifier<?> modifier = pool.obtain();
            modifier.setNodeData(nodeData);
            return modifier;
        }
    }
}
