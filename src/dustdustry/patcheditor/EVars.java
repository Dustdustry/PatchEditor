package dustdustry.patcheditor;

import dustdustry.patcheditor.core.resolve.*;
import mindustry.*;
import mindustry.mod.Mods.*;

public class EVars{
    public static LoadedMod thisMod;

    public static String githubNotesRepo = "Dustdustry/PatchNotes";
    public static String githubNotesBranch = "main";
    public static String repoLink = "https://github.com/Dustdustry/PatchEditor";

    public static void init(){
        thisMod = Vars.mods.getMod(Main.class);

        ObjectResolver.patch = new PatchResolution();
        ObjectResolver.content = new ContentResolution();
    }
}
