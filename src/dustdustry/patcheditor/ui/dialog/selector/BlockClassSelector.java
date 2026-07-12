package dustdustry.patcheditor.ui.dialog.selector;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import dustdustry.patcheditor.utils.*;
import mindustry.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.world.*;

import java.security.*;

public class BlockClassSelector extends SelectorDialog<Class<?>>{
    protected ObjectMap<Class<?>, Seq<Block>> map;

    public int maxBlocks = 10;

    public BlockClassSelector(){
        super("@selector.block-class");
    }

    @Override
    protected void setupItemTable(Table table, Class<?> item){
        table.add(item.getSimpleName()).style(Styles.outlineLabel);

        Seq<Block> blocks = map.get(item);
        if(blocks != null && !blocks.isEmpty()){
            table.row();
            table.table(blocksTable -> {
                for(int i = 0; i < Math.min(maxBlocks, blocks.size); i++){
                    Block block = blocks.get(i);
                    blocksTable.image(block.uiIcon).size(Vars.iconSmall).padLeft(i == 0 ? 0f : 8f).tooltip(block.localizedName);
                }
                if(blocks.size > maxBlocks){
                    blocksTable.add("...").padLeft(8f);
                }
            }).padTop(8f);
        }
    }

    @Override
    protected boolean matchQuery(Class<?> item){
        return Strings.matches(query, item.getName());
    }

    @Override
    protected Seq<Class<?>> getItems(){
        return map.keys().toSeq();
    }

    @Override
    protected void resetSelect(){
        super.resetSelect();

        for(var entry : map){
            entry.value.clear();
        }
        map.clear();
    }

    public void select(Boolf<Class<?>> consumer){
        select(Block.class, consumer);
    }

    public void select(Class<?> superClass, Boolf<Class<?>> consumer){
        select(null, superClass, consumer);
    }

    public void select(@Nullable Boolf<Class<?>> selectable, Class<?> superClass, Boolf<Class<?>> consumer){
        if(!Block.class.isAssignableFrom(superClass)){
            throw new InvalidParameterException("SuperClass cannot be assigned to Block.");
        }

        if(map == null) map = new OrderedMap<>();
        for(Block block : Vars.content.blocks()){
            Class<?> blockClass = ClassHelper.unoymousClass(block.getClass());
            if(selectable != null && !selectable.get(blockClass)) continue;
            if(ClassMap.classes.findKey(blockClass, true) != null){
                map.get(blockClass, Seq::new).add(block);
            }
        }

        super.select(consumer);
    }
}
