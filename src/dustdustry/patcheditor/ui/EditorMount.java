package dustdustry.patcheditor.ui;

import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.JsonWriter.*;
import dustdustry.patcheditor.ui.editor.*;
import dustdustry.patcheditor.ui.editor.PatchManager.*;
import dustdustry.patcheditor.utils.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.editor.data.*;
import mindustry.gen.*;
import mindustry.mod.data.*;
import mindustry.ui.*;

import static mindustry.Vars.state;

public class EditorMount{
    private static PatchEditor patchEditor;
    private static Seq<EditorPatch> editorPatches;

    public static void mount(){
        patchEditor = new PatchEditor();
        editorPatches = new Seq<>();

        MapInfoDialog infoDialog = Reflect.get(Vars.ui.editor, "infoDialog");
        MapAssetsDialog assetsDialog = Reflect.get(infoDialog, "patches");
        Table assetList = Reflect.get(assetsDialog, "list");

        assetsDialog.cont.addAction(Actions.forever(Actions.run(() -> {
            if(assetList.find("patch-editor-hook") == null){
                Element spyElement = new Element();
                spyElement.name = "patch-editor-hook";
                assetList.addChild(spyElement);

                DataAssetType currentType = Reflect.get(assetsDialog, "currentType");
                if(currentType == DataAssetType.patch){
                    mountPatch(assetsDialog, assetList);
                }else if(currentType == DataAssetType.content){
                    mountContent(assetsDialog, assetList);
                }
            }
        })));

        patchEditor.hidden(() -> {
            state.data.reloadPatches(editorPatches.map(e -> new PatchAsset(e.patch)));
            Reflect.invoke(assetsDialog, "rebuild");
        });
    }

    private static void mountPatch(MapAssetsDialog assetsDialog, Table assetList){
        editorPatches.set(Vars.state.data.getPatches().map(EditorPatch::new));
        TableUtils.insertColumnAfter(assetList, 1, t -> {
            for(EditorPatch editorPatch : editorPatches){
                t.button(Icon.edit, Styles.cleari, () -> {
                    state.data.reloadPatches(new Seq<>());
                    patchEditor.resetEditor();
                    patchEditor.edit(editorPatch, () -> {
                        state.data.reloadPatches(editorPatches.map(p -> new PatchAsset(p.patch)));
                        Reflect.invoke(assetsDialog, "rebuild");
                    });
                });
            }
        });

        assetList.row();
        assetList.add();
        assetList.button("### 添加空补丁包", Styles.cleart, () -> {
            Seq<PatchAsset> assets = state.data.getPatches();

            String name = findPatchName(assets);
            JsonValue json = new JsonValue(ValueType.object);
            json.addChild("name", new JsonValue(name));
            assets.add(new PatchAsset(json.toJson(OutputType.json)));

            state.data.reloadPatches(assets);
            Reflect.invoke(assetsDialog, "rebuild");
        }).padTop(12f).height(64f).colspan(assetList.getColumns()).fillX();
    }

    private static void mountContent(MapAssetsDialog assetsDialog, Table assetList){

    }

    private static String findPatchName(Seq<PatchAsset> patchAssets){
        String base = "Patch";

        int index = 0;
        while(true){
            String name = base + index;
            if(patchAssets.contains(p -> name.equals(p.name))){
                index++;
            }else{
                return name;
            }
        }
    }
}
