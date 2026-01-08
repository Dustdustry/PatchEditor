package MinRi2.PatchEditor.node.modifier;

import MinRi2.PatchEditor.node.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
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
        // field specific first
        new ModifierConfig(WeaponNameModifier::new, String.class).fieldOf(Weapon.class, "name"),

        new ModifierConfig(ColorModifier::new, Color.class),
        new ModifierConfig(ContentTypeModifier::new,
        Block.class, Item.class, Liquid.class, StatusEffect.class, UnitType.class),
        new ModifierConfig(BooleanModifier::new, Boolean.class, boolean.class),

        new ModifierConfig(TextureRegionModifier::new, TextureRegion.class),
        new ModifierConfig(StringModifier::new, String.class),
        new ModifierConfig(NumberModifier::new,
        Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
        byte.class, short.class, int.class, long.class, float.class, double.class)
        );
    }

    public static DataModifier<?> getModifier(ObjectNode node){
        if(canModify(node)){
            Class<?> type = node.type;
            for(ModifierConfig config : modifyConfig){
                if(config.canModify(node, type)) return config.getModifier();
            }
        }
        return null;
    }

    public static int getModifierIndex(ObjectNode node){
        if(canModify(node)){
            int i = 0;
            Class<?> type = node.type;
            for(ModifierConfig config : modifyConfig){
                if(config.canModify(node, type)) return i;
                i++;
            }
        }
        return -1;
    }

    public static boolean canModify(ObjectNode node){
        return node != null && node.hasSign(ModifierSign.MODIFY);
    }

    public static class ModifierConfig{
        public final Seq<Class<?>> modifierTypes = new Seq<>();
        private final Prov<DataModifier<?>> prov;

        private @Nullable Boolf<ObjectNode> nodeCheck;

        public ModifierConfig(Prov<DataModifier<?>> prov, Class<?>... types){
            this.prov = prov;
            modifierTypes.addAll(types);
        }

        public boolean canModify(ObjectNode node, Class<?> type){
            return (nodeCheck == null || nodeCheck.get(node)) && modifierTypes.contains(c -> c.isAssignableFrom(type));
        }

        public ModifierConfig check(Boolf<ObjectNode> extraCheck){
            this.nodeCheck = extraCheck;
            return this;
        }

        public ModifierConfig fieldOf(Class<?> clazz, String name){
            return check(node -> node.getParent() != null && clazz.isAssignableFrom(node.getParent().type) && name.equals(node.name));
        }

        public DataModifier<?> getModifier(){
            return prov.get();
        }
    }
}
