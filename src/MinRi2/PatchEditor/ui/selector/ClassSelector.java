package MinRi2.PatchEditor.ui.selector;

import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;

public class ClassSelector extends SelectorDialog<Class<?>>{
    private final Seq<Class<?>> classes = new Seq<>();

    public ClassSelector(){
        super("@selector.class");
    }

    @Override
    protected void setupItemTable(Table table, Class<?> item){
        table.add(item.getSimpleName());
    }

    @Override
    protected Seq<Class<?>> getItems(){
        return classes;
    }

    @Override
    protected boolean matchQuery(Class<?> item){
        return Strings.matches(query, item.getName());
    }

    @Override
    protected void resetSelect(){
        super.resetSelect();

        classes.clear();
    }

    public void select(Boolf<Class<?>> selectable, Boolf<Class<?>> consumer){
        select(selectable, null, consumer);
    }

    public void select(Boolf<Class<?>> selectable, Class<?> superClass, Boolf<Class<?>> consumer){
        classes.clear();
        for(var entry : ClassMap.classes){
            Class<?> clazz = entry.value;
            if(superClass != null && !superClass.isAssignableFrom(clazz)) continue;
            if(selectable != null && !selectable.get(clazz)) return;
            classes.add(clazz);
        }

        if(classes.isEmpty()){
            consumer.get(superClass);
            return;
        }else if(classes.size == 1 && classes.first() == superClass){
            consumer.get(superClass);
            return;
        }

        classes.sortComparing(Class::getSimpleName);

        super.select(consumer);
    }
}
