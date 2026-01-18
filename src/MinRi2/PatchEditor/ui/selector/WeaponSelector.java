package MinRi2.PatchEditor.ui.selector;

import MinRi2.PatchEditor.node.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.type.*;

public class WeaponSelector extends SelectorDialog<Weapon>{
    public WeaponSelector(){
        super("@selector.weapon");
    }

    @Override
    protected void setupItemTable(Table table, Weapon item){
        table.image(item.region).scaling(Scaling.fit).size(Vars.iconXLarge).pad(8f);
        table.add(item.name).ellipsis(true).wrap().pad(8f).growX();
    }

    @Override
    protected Seq<Weapon> getItems(){
        return PatchSelectList.getWeapons();
    }

    @Override
    protected boolean matchQuery(Weapon item){
        return Strings.matches(query, item.name);
    }
}
