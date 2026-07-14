package dustdustry.patcheditor.ui;

import arc.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
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
import mindustry.ctype.*;
import mindustry.editor.*;
import mindustry.editor.data.*;
import mindustry.gen.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class EditorMount{
    private static Seq<EditorPatch> editorPatches;

    // extremely hacky
    public static void mount(){
        editorPatches = new Seq<>();

        MapInfoDialog infoDialog = Reflect.get(Vars.ui.editor, "infoDialog");
        MapAssetsDialog assetsDialog = Reflect.get(infoDialog, "patches");
        Table assetList = Reflect.get(assetsDialog, "list");

        BaseDialog paused = Vars.ui.paused;
        paused.shown(() -> {
            if(!Vars.net.client()){
                paused.cont.row();
                paused.cont.button("@asset-dialog", Icon.edit, () -> {
                    ui.editor.build();
                    assetsDialog.show();
                }).padTop(8f).tooltip("@patch-editor.editInGame.info", true)
                .disabled(e -> Vars.net.client());
            }
        });

        assetsDialog.shown(() -> {
            if(!Core.settings.getBool("patch-editor.readUsageHint")){
                Core.app.post(() -> EUI.showUsageInfo(() -> {
                    Core.settings.put("patch-editor.readUsageHint", true);
                }));
            }
        });

        assetsDialog.cont.addAction(Actions.forever(Actions.run(() -> {
            if(assetList.find("patch-editor-hook") == null){
                Element spyElement = new Element();
                spyElement.name = "patch-editor-hook";
                assetList.addChild(spyElement);

                try{
                    DataAssetType currentType = Reflect.get(assetsDialog, "currentType");
                    if(currentType == DataAssetType.patch){
                        mountPatch(assetsDialog, assetList);
                    }else if(currentType == DataAssetType.content){
                        mountContent(assetsDialog, assetList);
                    }
                }catch(Exception e){
                    Vars.ui.showException(Core.bundle.get("patch-editor.mount.error"), e);
                }
            }
        })));

        EUI.patchEditor.hidden(() -> {
            state.data.reloadPatches(editorPatches.map(e -> new PatchAsset(e.patch)));
            Reflect.invoke(assetsDialog, "rebuild");
        });
    }

    private static void mountPatch(MapAssetsDialog assetsDialog, Table assetList){
        editorPatches.set(state.data.getPatches().map(EditorPatch::new));
        TableUtils.insertColumnAfter(assetList, 1, t -> {
            t.defaults().set(assetList.defaults()).fill();
            for(EditorPatch editorPatch : editorPatches){
                t.button(Icon.edit, Styles.graySquarei, iconMed, () -> {
                    state.data.reloadPatches(new Seq<>());
                    EUI.patchEditor.resetEditor();
                    EUI.patchEditor.edit(editorPatch, () -> {
                        state.data.reloadPatches(editorPatches.map(p -> new PatchAsset(p.patch)));
                        Reflect.invoke(assetsDialog, "rebuild");
                    });
                });
            }
        });

        assetList.row();
        if(editorPatches.any()) assetList.add();
        assetList.button("@patch-editor.addEmptyPatch", Icon.add, Styles.grayt, () -> {
            Seq<PatchAsset> assets = state.data.getPatches();

            String name = findPatchName(assets);
            JsonValue json = new JsonValue(ValueType.object);
            json.addChild("name", new JsonValue(name));
            assets.add(new PatchAsset(json.toJson(OutputType.json)));

            state.data.reloadPatches(assets);
            Reflect.invoke(assetsDialog, "rebuild");
        }).padTop(12f).minWidth(240f).height(64f).colspan(assetList.getColumns()).fillX();
    }

    private static void mountContent(MapAssetsDialog assetsDialog, Table assetList){
        Seq<ContentAsset> assets = state.data.getContent();

        Seq<Cell> cells = assetList.getCells().select(c -> c.get() instanceof ImageButton ib && ib.getImage().getDrawable() == Icon.refresh);
        if(cells.size != assets.size) return;

        for(int i = 0; i < assets.size; i++){
            ContentAsset asset = assets.get(i);
            Cell cell = cells.get(i);

            ImageButton button = new ImageButton(Icon.edit, Styles.graySquarei);
            button.resizeImage(iconMed);
            button.clicked(() -> {
                EUI.contentAssetEditor.show(asset, () -> Reflect.invoke(assetsDialog, "rebuild"));
            });

            cell.setElement(button);
        }

        String addText = Core.bundle.get("add");
        Cell<?> cell = assetsDialog.buttons.getCells().find(c -> {
            if(!(c.get() instanceof TextButton tb)) return false;
            return addText.contentEquals(tb.getText());
        });
        cell.setElement(new TextButton("@add"){{
            add(new Image(Icon.add)).size(Icon.add.imageSize());
            getCells().reverse();
            clicked(() -> {
                state.data.getContent().add(new ContentAsset("item1.json", ContentType.item, "{}"));
                state.data.reloadContent(false);
                state.data.regenerateContentSprites(false);
                Reflect.invoke(assetsDialog, "rebuild");
            });
        }});
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
