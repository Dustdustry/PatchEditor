package dustdustry.patcheditor;

import mindustry.*;
import mindustry.core.*;
import mindustry.mod.Mods.*;

public class EVars{
    public static LoadedMod thisMod;

    public static boolean hasAmoType = Version.build < 158;

    public static void init(){
        thisMod = Vars.mods.getMod(Main.class);
    }
}
