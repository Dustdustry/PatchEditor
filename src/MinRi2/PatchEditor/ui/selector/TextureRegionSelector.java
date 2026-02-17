package MinRi2.PatchEditor.ui.selector;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.ui.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.graphics.*;

public class TextureRegionSelector extends SelectorDialog<AtlasRegion>{
    public TextureRegionSelector(){
        super("@selector.texture-region");
    }

    @Override
    protected void setupCont(Table cont){
        float width = layoutWidth();
        int columns = (int)(width / 360f);

        ObjectMap<Character, Seq<AtlasRegion>> map = new OrderedMap<>();
        for(AtlasRegion item : getItems()){
            char letter = item.name.charAt(0);
            map.get(Character.toUpperCase(letter), Seq::new).add(item);
        }

        for(Entry<Character, Seq<AtlasRegion>> entry : map){
            char letter = entry.key;
            Seq<AtlasRegion> regions = entry.value;

            cont.table(t -> {
                t.image().color(Pal.darkerGray).size(32f, 6f);
                t.add("" + letter).color(EPalettes.type).padLeft(16f).padRight(16f).left();
                t.image().color(Pal.darkerGray).height(4f).growX();
            }).marginTop(16f).marginBottom(8f).growX();
            cont.row();
            Table regionsCont = cont.table().growX().get();
            cont.row();

            int index = 0;
            for(AtlasRegion region : regions){
                if(!query.isEmpty() && !matchQuery(region)) continue;

                regionsCont.button(table -> {
                    table.table(t -> setupItemTable(t, region)).growX();

                    table.image().width(4f).color(Color.darkGray).growY().right();
                    table.row();
                    Cell<?> horizontalLine = table.image().height(4f).color(Color.darkGray).growX();
                    horizontalLine.colspan(table.getColumns());
                }, EStyles.cardButtoni, () -> {
                    if(consumer.get(region)){
                        hide();
                    }
                }).pad(8f).growX();

                if(++index % columns == 0){
                    regionsCont.row();
                }
            }
        }
    }

    @Override
    protected void setupItemTable(Table table, AtlasRegion item){
        table.image(item).scaling(Scaling.fit).size(Vars.iconXLarge).pad(8f);
        table.add(item.name).ellipsis(true).wrap().pad(8f).growX();
    }

    @Override
    protected Seq<AtlasRegion> getItems(){
        return EditorList.getRegions();
    }

    @Override
    protected boolean matchQuery(AtlasRegion item){
        return Strings.matches(query, item.name);
    }
}
