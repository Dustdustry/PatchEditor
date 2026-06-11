package dustdustry.patcheditor;

import mindustry.*;
import mindustry.mod.Mods.*;

public class EVars{
    public static LoadedMod thisMod;
    public static boolean hasAmoType;

    public static String githubNotesRepo = "Dustdustry/PatchNotes";
    public static String githubNotesBranch = "main";

    public static void init(){
        thisMod = Vars.mods.getMod(Main.class);

        try{
            Class.forName("mindustry.type.AmmoType");
            hasAmoType = true;
        }catch(Exception e){
            hasAmoType = false;
        }
    }
}
