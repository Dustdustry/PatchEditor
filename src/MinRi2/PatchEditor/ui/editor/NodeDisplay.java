package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class NodeDisplay{
    public static final float labelWidth = 100f;
    public static final float imageSize = Vars.iconLarge;
    private static ObjectMap<ContentType, TextureRegion> contentSymbolMap;

    private static Table table;
    private static EditorNode node;

    private static void intiSymbol(){
        contentSymbolMap = ObjectMap.of(
        ContentType.item, Items.copper.uiIcon,
        ContentType.block, Blocks.sand.uiIcon,
        ContentType.liquid, Liquids.water.uiIcon,
        ContentType.status, StatusEffects.overclock.uiIcon,
        ContentType.unit, UnitTypes.alpha.uiIcon,
        ContentType.planet, Icon.icons.get(Planets.serpulo.icon).getRegion()
        );
    }

    private static void set(Table table, EditorNode node){
        NodeDisplay.table = table;
        NodeDisplay.node = node;
    }

    private static void reset(){
        table = null;
        node = null;
    }

    public static TextureRegion getDisplayIcon(Object object){
        if(object == null) return Icon.none.getRegion();
        if(object instanceof ContentType type) return contentSymbolMap.get(type, Icon.effect.getRegion());
        if(object instanceof UnlockableContent unlockable) return unlockable.uiIcon;
        if(object instanceof Weapon weapon) return Core.atlas.find(weapon.name, Icon.none.getRegion());
        if(object instanceof ItemStack stack) return stack.item.uiIcon;
        if(object instanceof LiquidStack stack) return stack.liquid.uiIcon;
        if(object instanceof PayloadStack stack) return stack.item.uiIcon;
        return Icon.effect.getRegion();
    }

    public static String getDisplayName(Object object){
        if(object instanceof ContentType type) return Strings.capitalize(type.name());
        if(object instanceof Content content){
            return content instanceof UnlockableContent unlockable ? unlockable.localizedName
            : content instanceof MappableContent mappable ? mappable.name
            : String.valueOf(content);
        }
        if(object instanceof Weapon weapon) return weapon.name;

        return String.valueOf(object);
    }

    public static void display(Table table, EditorNode node){
        set(table, node);
        displayObject(node.getDisplayValue());
        reset();
    }

    public static void displayNameType(Table table, EditorNode node){
        set(table, node);
        displayNameType(node.getDisplayValue());
        reset();
    }

    private static void displayObject(Object object){
        if(object == null){
            displayNameType(object);
            table.add().expandX();
            table.table(t -> {
                t.image(Icon.none).size(imageSize).row();
                t.add("null").padTop(8f);
            });
        }else if(object instanceof UnlockableContent || object instanceof Weapon){
            displayNameType(object);
            table.add().expandX();
            displayInfo(object);
        }else if(object instanceof ContentType contentType && contentType.contentClass != null){
            displayNameType(object);
            table.add().expandX();

            Seq<?> seq = Vars.content.getBy(contentType);
            if(seq.isEmpty()) return;
            if(contentSymbolMap == null) intiSymbol();
            displayInfo(contentType);
        }else if(object instanceof ItemStack || object instanceof LiquidStack || object instanceof PayloadStack){
            displayNameType(object);
            table.add().expandX();
            displayStack(object);
        }else{
            displayNameType(object);
        }
    }

    private static void displayNameType(Object object){
        table.table(nodeInfoTable -> {
            nodeInfoTable.defaults().minWidth(labelWidth).growX();

            Class<?> type = ClassHelper.unoymousClass(object == null ? node.getTypeOut() : object.getClass());
            nodeInfoTable.add(node.getDisplayName()).wrap().tooltip(node.getDisplayName());
            nodeInfoTable.row();
            nodeInfoTable.add(ClassHelper.getDisplayName(type)).fontScale(0.85f).color(EPalettes.type).ellipsis(true).wrap().padTop(4f).tooltip(ClassHelper.getDisplayName(type));
        });
    }

    private static void displayInfo(Object object){
        table.table(valueTable -> {
            valueTable.defaults().right();

            valueTable.image(getDisplayIcon(object)).scaling(Scaling.fit).size(imageSize);
            valueTable.row();
            valueTable.add(getDisplayName(object)).labelAlign(Align.right).ellipsis(true).wrap().padTop(8f).minWidth(labelWidth).growX();
        });
    }

    private static void displayStack(Object stack){
        table.table(valueTable -> {
            valueTable.defaults().right();

            float displayAmount = stack instanceof ItemStack itemStack ? itemStack.amount
            : stack instanceof LiquidStack liquidStack ? liquidStack.amount * 60f
            : stack instanceof PayloadStack payloadStack ? payloadStack.amount : 0;

            valueTable.stack(new Image(getDisplayIcon(stack)){{
                setScaling(Scaling.fit);
            }}, new Table(t -> {
                t.right().bottom();
                t.add(Strings.autoFixed(displayAmount, 2)).fontScale(0.9f).style(Styles.outlineLabel);
            })).size(imageSize);
        });
    }
}
