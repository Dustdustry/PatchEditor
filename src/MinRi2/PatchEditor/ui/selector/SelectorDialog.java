package MinRi2.PatchEditor.ui.selector;

import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.ui.dialogs.*;

public abstract class SelectorDialog<T> extends BaseDialog{
    protected Boolf<T> consumer;

    public SelectorDialog(String title){
        super(title);

        resized(this::rebuild);
        shown(this::rebuild);
        hidden(this::resetSelect);
        addCloseButton();
    }

    protected void rebuild(){
        cont.clearChildren();

        Table pane = new Table();

        float width = Core.scene.getWidth() * (Core.scene.getWidth() > 1000 ? 0.8f : 0.95f);
        cont.pane(pane).scrollX(false).width(width).grow();

        int index = 0, columns = (int)(width / 360f);
        for(T item : getItems()){
            pane.button(table -> {
                setupItemTable(table, item);
            }, EStyles.cardButtoni, () -> {
                if(consumer.get(item)){
                    hide();
                }
            }).pad(8f).growX();

            if(++index % columns == 0){
                pane.row();
            }
        }
    }

    protected abstract void setupItemTable(Table table, T item);

    protected abstract Seq<T> getItems();

    protected void resetSelect(){
        consumer = null;
    }

    public void select(Boolf<T> consumer){
        this.consumer = consumer;

        show();
    }
}
