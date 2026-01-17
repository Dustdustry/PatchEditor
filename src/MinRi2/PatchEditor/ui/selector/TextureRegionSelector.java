package MinRi2.PatchEditor.ui.selector;

import arc.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;

public class TextureRegionSelector extends SelectorDialog<AtlasRegion>{
    public TextureRegionSelector(){
        super("@texture-region-selector");
    }

    @Override
    protected void setupItemTable(Table table, AtlasRegion item){
        table.margin(8f);
        table.image(item).scaling(Scaling.fit).size(Vars.iconXLarge);
        table.add(item.name).ellipsis(true).wrap().pad(8f).growX();
    }

    @Override
    protected Seq<AtlasRegion> getItems(){
        // TODO: clarify by first letter
        return Core.atlas.getRegions().copy().sort(Structs.comparing(region -> region.name));
    }
}
