package dustdustry.patcheditor;

import mindustry.*;
import mindustry.mod.Mods.*;

public class EVars{
    public static LoadedMod thisMod;

    public static void init(){
        thisMod = Vars.mods.getMod(Main.class);
    }
}
