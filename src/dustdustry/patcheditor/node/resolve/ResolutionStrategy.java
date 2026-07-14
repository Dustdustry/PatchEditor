package dustdustry.patcheditor.node.resolve;

import dustdustry.patcheditor.node.*;
import dustdustry.patcheditor.node.EditorList.*;
import dustdustry.patcheditor.utils.*;
import arc.files.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import java.lang.reflect.*;
import mindustry.ai.types.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.part.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.type.weapons.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;

public abstract class ResolutionStrategy{

    protected Seq<Class<?>> classBlacklist = Seq.with(
    Class.class, Texture.class, Fi.class, KeyBind.class, UnitEntity.class, PartParams.class
    );
    protected ObjectMap<Class<?>, Seq<String>> fieldBlacklist = ObjectMap.of(
    Drill.class, Seq.with("oreCount", "itemArray"),
    UnitType.class, Seq.with("sample"),
    // Fields will be overridden after patched.
    RegionPart.class, Seq.with("regions", "outlines"),
    BasicBulletType.class, Seq.with("backRegion", "frontRegion"),
    PointLaserBulletType.class, Seq.with("laser", "laserEnd"),
    SapBulletType.class, Seq.with("laserRegion", "laserEndRegion"),
    Weapon.class, Seq.with("region", "heatRegion", "cellRegion", "outlineRegion"),
    RepairBeamWeapon.class, Seq.with("laser", "laserEnd", "laserTop", "laserTopEnd")
    );

    public void resolveRoot(ObjectNode node){
        var map = PatchJsonIO.getNameToType();
        for(ContentType ctype : ContentType.all){
            if(map.containsValue(ctype, true)){
                node.addChild(map.findKey(ctype, true), ctype, ContentType.class, ctype.contentClass, null);
            }
        }

        node.addChild("name", "Patch0").addSign(ModifierSign.MODIFY);
    }

    public boolean shouldAddPlusSign(ObjectNode node, Class<?> objectType){
        return true;
    }

    public boolean shouldAddModifySign(ObjectNode parent, ObjectNode child){
        return true;
    }

    public boolean shouldAddRemoveSign(ObjectNode parent, ObjectNode child){
        return true;
    }

    public void addSpecialChildren(ObjectNode node, Object object){
        if(object instanceof UnitType type){
            if(type.constructor != null){
                node.addChild("type", EditorList.getUnitTypeName(type.constructor.get().getClass()), UnitConstructorType.class).addSign(ModifierSign.MODIFY);
            }
            node.addChild("aiController", PatchJsonIO.getTypeName(type.aiController.get().getClass()), AIController.class).addSign(ModifierSign.MODIFY);
            node.addChild("controller", PatchJsonIO.getTypeName(CommandAI.class), AIController.class).addSign(ModifierSign.MODIFY);
        }

        if(object instanceof Block block){
            ObjectNode consumesNode = node.addChild("consumes", ObjectResolver.emptyObject, Consume.class);

            consumesNode.addChild("item", null, Item.class);
            consumesNode.addChild("itemCharged", ObjectExample.getExample(ConsumeItemCharged.class), ConsumeItemCharged.class);
            consumesNode.addChild("itemFlammable", ObjectExample.getExample(ConsumeItemFlammable.class), ConsumeItemFlammable.class);
            consumesNode.addChild("itemRadioactive", ObjectExample.getExample(ConsumeItemRadioactive.class), ConsumeItemRadioactive.class);
            consumesNode.addChild("itemExplosive", ObjectExample.getExample(ConsumeItemExplosive.class), ConsumeItemExplosive.class);
            consumesNode.addChild("itemList", ObjectExample.getExample(ConsumeItemList.class), ConsumeItemList.class);
            consumesNode.addChild("itemExplode", ObjectExample.getExample(ConsumeItemExplode.class), ConsumeItemExplode.class);
            consumesNode.addChild("items", ObjectExample.getExample(ConsumeItems.class), ConsumeItems.class);

            consumesNode.addChild("liquidFlammable", ObjectExample.getExample(ConsumeLiquidFlammable.class), ConsumeLiquidFlammable.class);
            consumesNode.addChild("liquid", ObjectExample.getExample(ConsumeLiquid.class), ConsumeLiquid.class);
            consumesNode.addChild("liquids", ObjectExample.getExample(ConsumeLiquids.class), ConsumeLiquids.class);
            consumesNode.addChild("coolant", ObjectExample.getExample(ConsumeCoolant.class), ConsumeCoolant.class);
            consumesNode.addChild("power", ObjectExample.getExample(ConsumePower.class), ConsumePower.class);
            consumesNode.addChild("powerBuffered", 0f, float.class);

            consumesNode.addSign(ModifierSign.MODIFY);
            for(ObjectNode child : consumesNode.getChildren().values()){
                child.addSign(ModifierSign.MODIFY);
            }

            ObjectNode removeNode = consumesNode.addChild("remove", ObjectResolver.emptyObject, Consume.class);
            removeNode.addSign(ModifierSign.MODIFY);

            ObjectSet<String> keysRemovable = new ObjectSet<>();
            for(Consume consumer : block.consumers){
                String type = ClassHelper.unoymousClass(consumer.getClass()).getSimpleName().replace("Consume", "");
                keysRemovable.add(Strings.camelToKebab(type));
            }

            if(!keysRemovable.isEmpty()) keysRemovable.add("all");
            for(String key : keysRemovable){
                ObjectNode removeConsumeNode = removeNode.addChild(key, ObjectResolver.emptyObject);
                removeConsumeNode.addSign(ModifierSign.MODIFY);
                removeConsumeNode.addSign(ModifierSign.REMOVE);
            }
        }
    }

    public boolean isTypeEditable(Class<?> clazz){
        return clazz != null && (!clazz.isInterface() || clazz == Interp.class)
        && !(clazz.isSynthetic() || classBlacklist.contains(black -> black.isAssignableFrom(clazz)));
    }

    public boolean isTypeResolvable(Class<?> clazz){
        return clazz != null && !(clazz.isPrimitive() || Reflect.isWrapper(clazz))
        && !ClassHelper.isAbstractClass(clazz) && isTypeEditable(clazz);
    }

    public boolean isFieldResolvable(Field field){
        int modifiers = field.getModifiers();
        return (!field.getType().isPrimitive() || !Modifier.isFinal(modifiers))
        && isTypeEditable(field.getType())
        && !(field.isAnnotationPresent(NoPatch.class) || field.getDeclaringClass().isAnnotationPresent(NoPatch.class));
    }

    public Seq<String> getFieldBlacklist(Class<?> type){
        Class<?> current = type;
        while(current != Object.class){
            Seq<String> list = fieldBlacklist.get(current);
            if(list != null) return list;
            current = current.getSuperclass();
        }
        return null;
    }
}
