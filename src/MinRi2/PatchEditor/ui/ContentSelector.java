package MinRi2.PatchEditor.ui;

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
public class ContentSelector extends BaseDialog{
    private ContentType contentType;
    private Boolf<UnlockableContent> selectable, consumer;
    private Class<?> restrictClass;

    public ContentSelector(){
        super("@content-selector");

        addCloseButton();

        resized(this::rebuild);
        shown(this::rebuild);
    }

    private void rebuild(){
        cont.clearChildren();

        Table pane = new Table();

        float width = Core.scene.getWidth() * (Core.scene.getWidth() > 1000 ? 0.8f : 0.95f);
        cont.pane(pane).scrollX(false).width(width).grow();

        Seq<UnlockableContent> seq = Vars.content.getBy(contentType);

        int index = 0, columns = (int)(width / 360f);
        for(UnlockableContent content : seq){
            if(restrictClass != null && !restrictClass.isAssignableFrom(content.getClass())) continue;
            if(!selectable.get(content)) continue;

            pane.button(table -> {
                setupContentTable(table, content);
            }, EStyles.cardButtoni, () -> {
                if(consumer.get(content)){
                    hide();
                }
            }).pad(8f).growX();

            if(++index % columns == 0){
                pane.row();
            }
        }
    }

    private void setupContentTable(Table table, UnlockableContent content){
        table.image(content.uiIcon).scaling(Scaling.fit).size(48f).pad(8f).expandX().left();

        table.table(infoTable -> {
            infoTable.right();
            infoTable.defaults().pad(4f).right();

            infoTable.add(content.localizedName);
            infoTable.row();
            infoTable.add(content.name).color(EPalettes.grayFront);
        });

        table.image().width(4f).color(Color.darkGray).growY().right();
        table.row();
        Cell<?> horizontalLine = table.image().height(4f).color(Color.darkGray).growX();
        horizontalLine.colspan(table.getColumns());
    }

    public void select(ContentType contentType, Boolf<UnlockableContent> selectable, Boolf<UnlockableContent> consumer){
        select(contentType, null, selectable, consumer);
    }

    public void select(ContentType contentType, Class<?> restrictClass, Boolf<UnlockableContent> selectable, Boolf<UnlockableContent> consumer){
        this.contentType = contentType;
        this.restrictClass = restrictClass;
        this.selectable = selectable;
        this.consumer = consumer;

        show();
    }
}
