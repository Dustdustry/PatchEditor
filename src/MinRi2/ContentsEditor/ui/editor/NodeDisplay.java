package MinRi2.ContentsEditor.ui.editor;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.ui.*;
import arc.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;

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

    public static String getDisplayName(Object object){
        if(object instanceof UnlockableContent content){
            return content.localizedName;
        }

        return "";
    }

    public static void display(Table table, EditorNode node){
        set(table, node);
        displayObject(node.getDisplayValue());
        reset();
    }

    public static void displayNameType(Table table, EditorNode node){
        set(table, node);
        displayNameType();
        reset();
    }

    private static void displayObject(Object object){
        if(object == null){
            displayNameType();
            table.add().expandX();
            table.table(t -> {
                t.image(Icon.none).size(imageSize).row();
                t.add("null").padTop(8f);
            });
        }else if(object instanceof UnlockableContent content){
            displayNameType();
            table.add().expandX();
            displayInfo(content.uiIcon, content.localizedName);
        }else if(object instanceof ContentType contentType
        && contentType.contentClass != null
        && UnlockableContent.class.isAssignableFrom(contentType.contentClass)){
            displayNameType();
            table.add().expandX();

            Seq<?> seq = Vars.content.getBy(contentType);
            if(seq.isEmpty()) return;
            if(contentSymbolMap == null) intiSymbol();
            TextureRegion icon = ((UnlockableContent)seq.first()).uiIcon;
            displayInfo(contentSymbolMap.get(contentType, icon), Strings.capitalize(contentType.name()));
        }else if(object instanceof Weapon weapon){
            displayNameType();
            table.add().expandX();
            displayInfo(Core.atlas.find(weapon.name, weapon.region), weapon.name);
        }else{
            displayNameType();
        }
    }

    private static void displayNameType(){
        table.table(nodeInfoTable -> {
            nodeInfoTable.defaults().width(labelWidth).left();

            Class<?> type = PatchJsonIO.getTypeOut(node);
            String typeName = type == null ? "unknown" : ClassHelper.getDisplayName(type);

            nodeInfoTable.add(node.name()).wrap().tooltip(node.name());
            nodeInfoTable.row();
            nodeInfoTable.add(typeName).fontScale(0.85f).color(EPalettes.type).ellipsis(true).padTop(4f).tooltip(typeName);
        });
    }

    private static void displayInfo(TextureRegion region, String value){
        displayInfo(region == null ? Icon.none : new TextureRegionDrawable(region), value);
    }

    private static void displayInfo(Drawable icon, String value){
        table.table(valueTable -> {
            valueTable.defaults().right();

            valueTable.image(icon).scaling(Scaling.fit).size(imageSize);
            valueTable.row();
            valueTable.add(value).labelAlign(Align.right).ellipsis(true).padTop(8f).width(labelWidth);
        });
    }
}
