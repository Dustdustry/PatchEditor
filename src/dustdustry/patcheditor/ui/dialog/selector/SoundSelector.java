package dustdustry.patcheditor.ui.dialog.selector;

import arc.math.*;
import arc.scene.ui.*;
import dustdustry.patcheditor.core.*;
import arc.*;
import arc.audio.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import dustdustry.patcheditor.core.EditorList.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;

import java.lang.reflect.*;

// TODO: add specified UI for sound asset.
public class SoundSelector extends SelectorDialog<SoundEntry>{
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
    protected void setupButtons(){
        buttons.button("@selector.sound.loadAll", Icon.download, () -> {
            for(SoundEntry item : getItems()){
                Sound sound = item.sound;
                long handle = Reflect.get(AudioSource.class, sound, "handle");
                if(handle == 0 && sound.file != null){
                    sound.load(sound.file);
                }
            }
            rebuild();
        }).tooltip("@selector.sound.loadAll.tooltip");
    }

    @Override
    protected void setupItemTable(Table table, SoundEntry item){
        Sound sound = item.sound;
        table.add(item.name).pad(4f).expandX().left();

        table.button(b -> {
            b.image(Icon.play).pad(4f);
            b.add("@selector.sound.unloaded").minWidth(64f).update(l -> {
                if(!Mathf.zero(sound.getLength())){
                    l.setText(Strings.autoFixed(sound.getLength(), 2) + "s");
                    l.update(null);
                }
            });
        }, Styles.clearNonei, () -> {
            AudioBus lastBus = sound.bus;
            sound.setBus(Vars.control.sound.uiBus);
            playingSounds.add(sound.play(Core.audio.sfxVolume));
            sound.setBus(lastBus);
        }).padRight(4f).growY();
    }

    @Override
    protected boolean matchQuery(SoundEntry item){
        return Strings.matches(query, item.name);
    }

    @Override
    protected Seq<SoundEntry> getItems(){
        return EditorList.getSoundEntries().sortComparing(e -> e.name);
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
