package dustdustry.patcheditor.core;

import mindustry.mod.data.*;

public class EditorPatch{
    public String name;
    public String patch;

    public EditorPatch(String name, String patch){
        this.name = name;
        this.patch = patch;
    }

    public EditorPatch(PatchAsset patchAsset){
        this(patchAsset.name, patchAsset.patch);
    }
}
