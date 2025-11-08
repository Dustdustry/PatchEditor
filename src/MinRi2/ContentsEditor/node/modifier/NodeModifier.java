package MinRi2.ContentsEditor.node.modifier;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.node.modifier.equal.*;
import arc.func.*;
import arc.struct.*;
import arc.util.pooling.*;
import mindustry.type.*;
import mindustry.world.*;

/**
 * @author minri2
 * Create by 2024/2/16
 */
public class NodeModifier{
    public static final Seq<ModifierConfig> modifyConfig = new Seq<>();

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
        return node.getChildren().containsKey(ModifierSign.MODIFY.sign);
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
            return modifierTypes.contains(NodeHelper.getType(node));
        }

        public DataModifier<?> getModifier(NodeData nodeData){
            DataModifier<?> modifier = pool.obtain();
            modifier.setNodeData(nodeData);
            return modifier;
        }
    }
}
