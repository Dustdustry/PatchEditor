package dustdustry.patcheditor.ui.dialog.selector;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import dustdustry.patcheditor.node.*;
import dustdustry.patcheditor.node.EditorList.*;
import dustdustry.patcheditor.ui.*;
import mindustry.*;
import mindustry.ui.*;

public class ColorSelector extends SelectorDialog<ColorEntry>{
    public ColorSelector(){
        super("@selector.color");
    }

    @Override
    protected void setupItemTable(Table table, ColorEntry item){
        table.left();

        BorderImage image = new BorderImage();
        image.thickness = 2f;
        table.add(image).color(item.color).size(Vars.iconMed).padRight(16f);
        table.add(item.name).minWidth(itemWidth * 0.4f);
        table.add(item.color.toString()).color(EPalettes.grayFront).padLeft(16f);
    }

    @Override
    protected boolean matchQuery(ColorEntry item){
        return Strings.matches(query, item.name);
    }

    @Override
    protected Seq<ColorEntry> getItems(){
        return EditorList.getColorList();
    }
}
