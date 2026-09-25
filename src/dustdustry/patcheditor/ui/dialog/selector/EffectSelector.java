package dustdustry.patcheditor.ui.dialog.selector;

import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.ui.*;
import dustdustry.patcheditor.ui.EffectElems.*;
import mindustry.*;
import mindustry.logic.LogicFx.*;
import mindustry.ui.*;

public class EffectSelector extends SelectorDialog<EffectEntry>{

    public EffectSelector(){
        super("@selector.effect");

        itemWidth = Vars.iconLarge * 6f;
    }

    @Override
    protected void setupItemTable(Table table, EffectEntry item){
        float size = itemWidth; // avoid IDEA check

        ClickListener listener = new ClickListener();
        Element element = EffectElems.getEffectElem(item, listener);
        table.add(element).growX().height(size).tooltip(item.name, true);
        element.addListener(listener);

        table.fill(t -> {
            t.right().bottom();
            t.add(item.name).ellipsis(true).labelAlign(Align.right).color(EPalettes.value).pad(8f).width(size * 0.75f);
        });
    }

    @Override
    protected boolean matchQuery(EffectEntry item){
        return Strings.matches(query, item.name);
    }

    @Override
    protected Seq<EffectEntry> getItems(){
        return EditorList.getEffectList();
    }
}
