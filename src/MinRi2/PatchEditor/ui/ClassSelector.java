package MinRi2.PatchEditor.ui;

import arc.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;

public class ClassSelector extends BaseDialog{
    private Boolf<Class<?>> consumer;
    private final Seq<Class<?>> classes = new Seq<>();

    public ClassSelector(){
        super("@class-selector");
        shown(this::rebuild);
        addCloseButton();
    }

    private void rebuild(){
        cont.clearChildren();

        Table pane = new Table();

        float width = Core.scene.getWidth() * (Core.scene.getWidth() > 1000 ? 0.8f : 0.95f);
        cont.pane(pane).scrollX(false).width(width).grow();

        int index = 0, columns = (int)(width / 240f);

        classes.sortComparing(Class::getSimpleName);
        for(Class<?> clazz : classes){
            pane.button(table -> {
                table.add(clazz.getSimpleName());
            }, EStyles.cardButtoni, () -> {
                if(consumer.get(clazz)){
                    hide();
                }
            }).height(48f).pad(8f).growX();

            if(++index % columns == 0){
                pane.row();
            }
        }

        classes.clear();
    }

    public void select(Boolf<Class<?>> selectable, Boolf<Class<?>> consumer){
        select(selectable, null, consumer);
    }

    public void select(Boolf<Class<?>> selectable, Class<?> superClass, Boolf<Class<?>> consumer){
        this.consumer = consumer;

        classes.clear();
        for(var entry : ClassMap.classes){
            Class<?> clazz = entry.value;
            if(superClass != null && !superClass.isAssignableFrom(clazz)) continue;
            if(selectable != null && !selectable.get(clazz)) return;
            classes.add(clazz);
        }

        if(classes.isEmpty()){
            consumer.get(superClass);
        }else if(classes.size == 1 && classes.first() == superClass){
            consumer.get(superClass);
        }else{
            show();
        }
    }
}
