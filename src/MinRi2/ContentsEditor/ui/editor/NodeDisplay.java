package MinRi2.ContentsEditor.ui.editor;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.ui.*;
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
    public static final float labelWidth = 120f;
    private static ObjectMap<ContentType, Drawable> contentSymbolMap;

    private static Table table;
    private static NodeData node;

    private static void intiSymbol(){
        contentSymbolMap = ObjectMap.of(
        ContentType.item, new TextureRegionDrawable(Items.copper.uiIcon),
        ContentType.block, new TextureRegionDrawable(Blocks.sand.uiIcon),
        ContentType.liquid, new TextureRegionDrawable(Liquids.water.uiIcon),
        ContentType.status, new TextureRegionDrawable(StatusEffects.overclock.uiIcon),
        ContentType.unit, new TextureRegionDrawable(UnitTypes.alpha.uiIcon),
        ContentType.planet, Icon.icons.get(Planets.serpulo.icon)
        );
    }

    private static void set(Table table, NodeData node){
        NodeDisplay.table = table;
        NodeDisplay.node = node;
    }

    public static void reset(){
        table = null;
        node = null;
    }

    public static String getDisplayName(Object object){
        if(object instanceof UnlockableContent content){
            return content.localizedName;
        }

        return "";
    }

    public static void display(Table table, NodeData node){
        set(table, node);

        if(node.object == null){
            displayName();
            return;
        }

        displayObject(node.object);
        reset();
    }

    public static void displayNameType(Table table, NodeData node){
        set(table, node);

        if(node.object == null){
            displayName();
            return;
        }

        displayNameType();
        reset();
    }

    private static void displayName(){
        table.add(node.name).wrap().width(labelWidth).pad(4f).expandX().left();
    }

    private static void displayObject(Object object){
        if(object == null){
            displayNameType();
            return;
        }

        if(object instanceof UnlockableContent content){
            displayNameType();
            displayInfo(new TextureRegionDrawable(content.uiIcon), content.localizedName);
        }else if(object instanceof ContentType contentType
        && contentType.contentClass != null
        && UnlockableContent.class.isAssignableFrom(contentType.contentClass)){
            displayContentType(contentType);
        }else if(object instanceof Weapon weapon){
            displayNameType();
            displayInfo(new TextureRegionDrawable(weapon.region), weapon.name);
        }else{
            displayNameType();
        }
    }


    private static void displayNameType(){
        table.table(nodeInfoTable -> {
            nodeInfoTable.defaults().expandX().left();

            Class<?> type = NodeHelper.getType(node);
            String typeName = type == null ? "unknown" : type.getSimpleName();

            nodeInfoTable.add(node.name).wrap().width(labelWidth);
            nodeInfoTable.row();
            nodeInfoTable.add(typeName).fontScale(0.85f).color(EPalettes.type).wrap().width(labelWidth).padTop(4f);
        }).pad(4f).left();
    }

    private static void displayInfo(Drawable icon, String value){
        table.table(valueTable -> {
            valueTable.defaults().expandX().right();

            valueTable.image(icon).scaling(Scaling.fit).size(Vars.iconLarge);
            valueTable.row();
            valueTable.add(value).labelAlign(Align.right).ellipsis(true).width(labelWidth).padTop(4f);
        }).width(labelWidth).pad(4f).right();
    }

    private static void displayContentType(ContentType contentType){
        displayNameType();

        Seq<?> seq = Vars.content.getBy(contentType);
        if(seq.isEmpty()){
            return;
        }

        if(contentSymbolMap == null){
            intiSymbol();
        }

        TextureRegion region = ((UnlockableContent)seq.first()).uiIcon;
        Drawable icon = new TextureRegionDrawable(region);
        icon = contentSymbolMap.get(contentType, icon);

        displayInfo(icon, Strings.capitalize(contentType.name()));
    }
}
