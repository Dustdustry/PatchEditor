package MinRi2.PatchEditor.ui.selector;

import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public abstract class SelectorDialog<T> extends BaseDialog{
    protected Boolf<T> consumer;
    protected String query = "";

    private Table itemCont;
    private ScrollPane pane;

    public SelectorDialog(String title){
        super(title);

        resized(this::rebuild);
        shown(this::rebuild);
        hidden(this::resetSelect);
        addCloseButton();
    }

    protected float layoutWidth(){
        return Core.scene.getWidth() * (Core.scene.getWidth() > 1000 ? 0.8f : 0.95f);
    }

    protected void rebuild(){
        if(itemCont == null) itemCont = new Table();
        if(pane == null) pane = new ScrollPane(itemCont);

        cont.clearChildren();
        cont.table(this::setupSearchTable).growX().row();
        cont.add(pane).scrollX(false).width(layoutWidth()).grow();

        itemCont.clearChildren();
        setupCont(itemCont);
    }

    protected void setupCont(Table cont){
        float width = layoutWidth();

        int index = 0, columns = (int)(width / 360f);
        for(T item : getItems()){
            if(!query.isEmpty() && !matchQuery(item)) continue;

            Button btn = cont.button(table -> {
                table.table(t -> setupItemTable(t, item)).pad(4f).growX();

                table.image().width(4f).color(Color.darkGray).growY().right();
                table.row();
                Cell<?> horizontalLine = table.image().height(4f).color(Color.darkGray).growX();
                horizontalLine.colspan(table.getColumns());
            }, EStyles.cardButtoni, () -> {}).pad(8f).growX().get();

            EUI.backButtonClick(btn, () -> {
                if(consumer.get(item)){
                    hide();
                }
            });

            if(++index % columns == 0){
                cont.row();
            }
        }
    }

    protected void setupSearchTable(Table table){
        table.image(Icon.zoom).pad(8f);
        TextField field = table.field(query, s -> {
            query = s;
            itemCont.clearChildren();
            setupCont(itemCont);
        }).growX().get();
        table.button(Icon.cancel, Styles.cleari, () -> {
            query = "";
            itemCont.clearChildren();
            setupCont(itemCont);
        }).pad(8f);

        field.update(() -> {
            if(!field.hasKeyboard()){
                field.requestKeyboard();
                field.setText(query);
            }
        });
    }

    protected abstract void setupItemTable(Table table, T item);

    protected abstract boolean matchQuery(T item);

    protected abstract Seq<T> getItems();

    protected void resetSelect(){
        consumer = null;
    }

    public void select(Boolf<T> consumer){
        this.consumer = consumer;

        show();
    }
}
