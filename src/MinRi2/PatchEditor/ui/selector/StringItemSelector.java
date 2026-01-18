package MinRi2.PatchEditor.ui.selector;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;

public class StringItemSelector extends SelectorDialog<String>{
    private final Seq<String> items = new Seq<>();

    public StringItemSelector(){
        super("@selector.stringItems");
    }

    @Override
    protected void setupItemTable(Table table, String item){
        table.add(item);
    }

    @Override
    protected boolean matchQuery(String item){
        return Strings.matches(query, item);
    }

    @Override
    protected Seq<String> getItems(){
        return items;
    }

    public void select(Seq<String> seq, Boolf<String> selectable, Boolf<String> consumer){
        items.set(seq);
        if(selectable != null) items.retainAll(selectable);
        super.select(consumer);
    }
}
