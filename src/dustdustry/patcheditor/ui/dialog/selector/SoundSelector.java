package dustdustry.patcheditor.ui.dialog.selector;

import dustdustry.patcheditor.node.*;
import arc.*;
import arc.audio.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;

import java.lang.reflect.*;

public class SoundSelector extends SelectorDialog<Sound>{
    private final IntSeq playingSounds = new IntSeq();

    public SoundSelector(){
        super("@selector.sound");

        hidden(() -> {
            playingSounds.each(handle -> {
                if(SoloudAssessor.idValid(handle)){
                    SoloudAssessor.idStopMethod(handle);
                }
            });
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
                    return false;
                }
            }

            try{
                return (boolean)idValidMethod.invoke(null, handle);
            }catch(IllegalAccessException | InvocationTargetException e){
                return false;
            }
        }

        public static void idStopMethod(int handle){
            if(idStopMethod == null){
                try{
                    idStopMethod = Soloud.class.getDeclaredMethod("idStop", int.class);
                    idStopMethod.setAccessible(true);
                }catch(NoSuchMethodException ignored){
                    return;
                }
            }

            try{
                idStopMethod.invoke(null, handle);
            }catch(IllegalAccessException | InvocationTargetException ignored){
            }
        }
    }
}
