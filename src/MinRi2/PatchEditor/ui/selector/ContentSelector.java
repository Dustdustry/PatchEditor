package MinRi2.PatchEditor.ui.selector;

import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.ui.editor.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import org.w3c.dom.*;

/**
 * @author minri2
 * Create by 2024/2/17
 */
public class ContentSelector extends SelectorDialog<Content>{
    private @Nullable ContentType contentType; // null means all the content
    private Boolf<Content> selectable;
    private @Nullable Class<?> restrictClass;

    public ContentSelector(){
        super("@selector.content");
    }

    @Override
    protected void setupItemTable(Table table, Content item){
        table.image(NodeDisplay.getDisplayIcon(item)).scaling(Scaling.fit).size(Vars.iconXLarge).pad(8f);

        table.table(infoTable -> {
            infoTable.defaults().pad(4f).right();

            infoTable.add(NodeDisplay.getDisplayName(item));
            if(item instanceof UnlockableContent unlockable){
                infoTable.row();
                infoTable.add(unlockable.name).color(EPalettes.grayFront);
            }
        }).expandX().right();
    }

    @Override
    protected boolean matchQuery(Content item){
        return Strings.matches(query, NodeDisplay.getDisplayName(item))
        || (item instanceof UnlockableContent unlockable && Strings.matches(query, unlockable.name));
    }

    @Override
    protected Seq<Content> getItems(){
        Seq<Content> contents = new Seq<>();
        if(contentType != null){
            contents.addAll(Vars.content.getBy(contentType));
        }else{
            Vars.content.each(contents::add);
        }
        return contents.retainAll(c -> (selectable == null || selectable.get(c)) && (restrictClass == null || restrictClass.isAssignableFrom(c.getClass())));
    }

    public void select(@Nullable ContentType contentType, Boolf<Content> selectable, Boolf<Content> consumer){
        select(contentType, null, selectable, consumer);
    }

    public void select(@Nullable ContentType contentType, @Nullable Class<?> restrictClass, Boolf<Content> selectable, Boolf<Content> consumer){
        this.contentType = contentType;
        this.restrictClass = restrictClass;
        this.selectable = selectable;

        super.select(consumer);
    }
}
