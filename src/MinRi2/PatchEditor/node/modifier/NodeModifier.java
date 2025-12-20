package MinRi2.PatchEditor.node.modifier;

import MinRi2.PatchEditor.node.*;
import arc.func.*;
import arc.graphics.*;
import arc.struct.*;
import mindustry.type.*;
import mindustry.world.*;

import static MinRi2.PatchEditor.node.modifier.EqualModifier.*;

/**
 * @author minri2
 * Create by 2024/2/16
 */
public class NodeModifier{
    public static final Seq<ModifierConfig> modifyConfig = new Seq<>();

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
        return node.getObjNode() != null && node.getObjNode().hasSign(ModifierSign.MODIFY);
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
