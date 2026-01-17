package MinRi2.PatchEditor.ui.selector;

import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.ui.dialogs.*;

/**
 * @author minri2
 * Create by 2024/2/17
 */
public class ContentSelector extends SelectorDialog<UnlockableContent>{
    private ContentType contentType;
    private Boolf<UnlockableContent> selectable;
    private Class<?> restrictClass;

    public ContentSelector(){
        super("@content-selector");
    }

    @Override
    protected Seq<UnlockableContent> getItems(){
        Seq<UnlockableContent> contents = Vars.content.getBy(contentType);
        return contents.select(c -> (selectable == null || selectable.get(c))
            && (restrictClass == null || restrictClass.isAssignableFrom(c.getClass()))
        );
    }

    @Override
    protected void setupItemTable(Table table, UnlockableContent item){
        table.image(item.uiIcon).scaling(Scaling.fit).size(48f).pad(8f).expandX().left();

        table.table(infoTable -> {
            infoTable.right();
            infoTable.defaults().pad(4f).right();

            infoTable.add(item.localizedName);
            infoTable.row();
            infoTable.add(item.name).color(EPalettes.grayFront);
        });
    }

    public void select(ContentType contentType, Boolf<UnlockableContent> selectable, Boolf<UnlockableContent> consumer){
        select(contentType, null, selectable, consumer);
    }

    public void select(ContentType contentType, Class<?> restrictClass, Boolf<UnlockableContent> selectable, Boolf<UnlockableContent> consumer){
        this.contentType = contentType;
        this.restrictClass = restrictClass;
        this.selectable = selectable;

        super.select(consumer);
    }
}
