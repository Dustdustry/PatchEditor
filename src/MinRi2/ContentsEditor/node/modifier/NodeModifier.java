package MinRi2.ContentsEditor.node.modifier;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.node.modifier.BaseModifier.*;
import MinRi2.ContentsEditor.node.modifier.equal.*;
import MinRi2.ContentsEditor.node.modifier.equal.EqualModifier.*;
import MinRi2.ContentsEditor.ui.*;
import MinRi2.ContentsEditor.ui.editor.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.actions.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.pooling.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import java.lang.reflect.*;

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

    public static BaseModifier<?> getModifier(NodeData node){
        for(ModifierConfig config : modifyConfig){
            if(config.canModify(node)) return config.getModifier(node);
        }
        return null;
    }

    public static int getModifierIndex(NodeData node){
        int i = 0;
        for(ModifierConfig config : modifyConfig){
            if(config.canModify(node)) return i;
            i++;
        }
        return -1;
    }

    public static class ModifierConfig{
        public final Seq<Class<?>> modifierTypes = new Seq<>();
        private final Pool<BaseModifier<?>> pool;

        @SuppressWarnings("unchecked")
        public ModifierConfig(Class<? extends BaseModifier<?>> clazz, Prov<? extends BaseModifier<?>> prov, Class<?>... types){
            pool = Pools.get((Class)clazz, prov);
            modifierTypes.addAll(types);
        }

        public boolean canModify(NodeData node){
            return modifierTypes.contains(NodeHelper.getType(node));
        }

        public BaseModifier<?> getModifier(NodeData nodeData){
            BaseModifier<?> modifier = pool.obtain();
            modifier.setNodeData(nodeData);
            return modifier;
        }
    }
}
