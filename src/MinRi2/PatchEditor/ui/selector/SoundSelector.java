package MinRi2.PatchEditor.ui.selector;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.audio.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.GameState.*;
import mindustry.gen.*;
import mindustry.ui.*;

import java.lang.invoke.*;
import java.lang.reflect.*;

import static mindustry.Vars.state;

public class SoundSelector extends SelectorDialog<Sound>{
    private final IntSeq playingSounds = new IntSeq();

    public SoundSelector(){
        super("@selector.sound");

        hidden(() -> {
            for(int handle : playingSounds.items){
                if(SoloudAssessor.idValid(handle)){
                    SoloudAssessor.idStopMethod(handle);
                }
            }
            playingSounds.clear();
        });
    }

    @Override
    protected void setupItemTable(Table table, Sound item){
        String name = PatchJsonIO.getKeyEntryMap(Sound.class).findKey(item, true);
        table.add(name).pad(4f).expandX().left();
        table.button(b -> {
            b.image(Icon.play).pad(4f);
            b.add(Strings.autoFixed(item.getLength(), 2) + "s").width(64f);
        }, Styles.clearNonei, () -> {
            AudioBus lastBus = item.bus;
            item.setBus(Vars.control.sound.uiBus);
            playingSounds.add(item.play(Core.audio.sfxVolume));
            item.setBus(lastBus);
        }).padRight(4f).growY();
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

    public static class SoloudAssessor{
        private static Method idValidMethod, idStopMethod;

        public static boolean idValid(int handle){
            if(idValidMethod == null){
                try{
                    idValidMethod = Soloud.class.getDeclaredMethod("idValid", int.class);
                    idValidMethod.setAccessible(true);
                }catch(NoSuchMethodException e){
                    throw new RuntimeException(e);
                }
            }

            try{
                return (boolean)idValidMethod.invoke(null, handle);
            }catch(IllegalAccessException | InvocationTargetException e){
                throw new RuntimeException(e);
            }
        }

        public static void idStopMethod(int handle){
            if(idStopMethod == null){
                try{
                    idStopMethod = Soloud.class.getDeclaredMethod("idStop", int.class);
                    idStopMethod.setAccessible(true);
                }catch(NoSuchMethodException e){
                    throw new RuntimeException(e);
                }
            }

            try{
                idStopMethod.invoke(null, handle);
            }catch(IllegalAccessException | InvocationTargetException e){
                throw new RuntimeException(e);
            }
        }
    }
}
