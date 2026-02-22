package MinRi2.PatchEditor.ui.selector;

import MinRi2.PatchEditor.node.*;
import arc.*;
import arc.audio.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.*;

public class SoundSelector extends SelectorDialog<Sound>{
    public SoundSelector(){
        super("@selector.sound");
    }

    @Override
    public void act(float delta){
        super.act(delta);

        Core.audio.soundBus.fadeFilterParam(0, Filters.paramWet, 0f, 0.4f);
    }

    @Override
    protected void setupItemTable(Table table, Sound item){
        String name = PatchJsonIO.getKeyEntryMap(Sound.class).findKey(item, true);
        table.add(name);
        table.button(Icon.play, Styles.clearNonei, () -> {
            item.play(Core.audio.sfxVolume);
        }).pad(4f);
    }

    @Override
    protected boolean matchQuery(Sound item){
        String name = PatchJsonIO.getKeyEntryMap(Sound.class).findKey(item, true);
        return Strings.matches(query, name);
    }

    @Override
    protected Seq<Sound> getItems(){
        return EditorList.getSoundList();
    }
}
