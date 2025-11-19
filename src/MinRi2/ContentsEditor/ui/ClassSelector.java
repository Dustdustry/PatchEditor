package MinRi2.ContentsEditor.ui;

import arc.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;

public class ClassSelector extends BaseDialog{
    private Boolf<Class<?>> selectable, consumer;
    private @Nullable Class<?> superClass;

    public ClassSelector(){
        super("@class-selector");
        shown(this::rebuild);
    }

    private void rebuild(){
        cont.clearChildren();

        Table pane = new Table();

        float width = Core.scene.getWidth() * (Core.scene.getWidth() > 1000 ? 0.8f : 0.95f);
        cont.pane(pane).scrollX(false).width(width).grow();

        int index = 0, columns = (int)(width / 80f);
        for(var entry : ClassMap.classes){
            Class<?> clazz = entry.value;
            if(superClass != null && !superClass.isAssignableFrom(clazz)) continue;
            if(selectable != null && !selectable.get(clazz)) return;

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
    }

    public void select(Boolf<Class<?>> selectable, Boolf<Class<?>> consumer){
        select(selectable, null, consumer);
    }

    public void select(Boolf<Class<?>> selectable, Class<?> superClass, Boolf<Class<?>> consumer){
        this.selectable = selectable;
        this.superClass = superClass;
        this.consumer = consumer;

        show();
    }
}
