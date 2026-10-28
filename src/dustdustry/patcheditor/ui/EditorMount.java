package dustdustry.patcheditor.ui;

import arc.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.EditorPatch;
import mindustry.*;
import mindustry.core.GameState.*;
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
    private static boolean contentMismatchWarned;

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

                    if(state.isGame() && !Core.settings.getBool("patch-editor.readInGameWarning")){
                        Core.app.post(() -> EUI.showInGameWarning(() -> {
                            Core.settings.put("patch-editor.readInGameWarning", true);
                        }));
                    }
                }));
            }else if(state.isGame() && !Core.settings.getBool("patch-editor.readInGameWarning")){
                Core.app.post(() -> EUI.showInGameWarning(() -> {
                    Core.settings.put("patch-editor.readInGameWarning", true);
                }));
            }
        });

        assetsDialog.cont.addAction(Actions.forever(Actions.run(() -> {
            Table buttons = assetsDialog.buttons;
            if(buttons.find("patch-editor-missing-regions-hook") == null){
                Element spyElement = new Element();
                spyElement.name = "patch-editor-missing-regions-hook";
                buttons.addChild(spyElement);

                buttons.button("@patch-editor.missingRegions.title", Icon.zoom, () -> {
                    EUI.missingRegions.show();
                });
            }

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

            String searchString = Reflect.get(assetsDialog, "searchString");
            Seq<EditorPatch> patchList = searchString == null ? editorPatches : editorPatches.select(patch -> patch.name.toLowerCase().contains(searchString));
            for(EditorPatch editorPatch : patchList){
                t.button(Icon.edit, Styles.graySquarei, iconMed, () -> {
                    state.data.reloadPatches(new Seq<>());
                    EUI.patchEditor.edit(editorPatch, () -> {
                        try{
                            state.data.reloadPatches(editorPatches.map(e -> new PatchAsset(e.patch)));
                            Reflect.invoke(assetsDialog, "rebuild");
                        }catch(Exception e){
                            Vars.ui.showException(e);
                        }

                        EUI.patchEditor.resetEditor();
                        if(state.isGame()){
                            try{
                                Vars.logic.update();
                            }catch(Exception e){
                                state.set(State.paused);
                                Vars.ui.showException("@patch-editor.editInGame.error", e);
                            }
                        }
                    });
                });
            }
        });

        assetList.row();
        if(editorPatches.any()) assetList.add();
        assetList.button("@patch-editor.addEmptyPatch", Icon.add, Styles.grayt, () -> {
            Seq<PatchAsset> assets = state.data.getPatches();

            String name = findAssetName("Patch", assets);
            Jval json = Jval.newObject();
            json.add("name", name);
            assets.add(new PatchAsset(json.toString(Jformat.plain)));

            state.data.reloadPatches(assets);
            Reflect.invoke(assetsDialog, "rebuild");
        }).padTop(12f).minWidth(240f).height(64f).colspan(assetList.getColumns()).fillX();
    }

    private static void mountContent(MapAssetsDialog assetsDialog, Table assetTable){
        Seq<ContentAsset> assets = state.data.getContent();

        String searchString = Reflect.get(assetsDialog, "searchString");
        Seq<ContentAsset> assetsList = searchString == null ? assets : assets.select(asset -> asset.name.toLowerCase().contains(searchString));

        Seq<Cell> cells = assetTable.getCells().select(c -> c.get() instanceof ImageButton ib && ib.getImage().getDrawable() == Icon.refresh);
        if(cells.size != assetsList.size){
            //行数对不上说明原版内容列表的结构变了。只提示一次，避免搜索时每敲一个字刷一条日志
            if(!contentMismatchWarned){
                contentMismatchWarned = true;
                Log.warn("PatchEditor: content list has @ row(s) but @ content asset(s), edit buttons were not mounted. The editor may need an update for this game version.", cells.size, assetsList.size);
            }
            return;
        }

        for(int i = 0; i < assetsList.size; i++){
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
                String assetName = findAssetName("item", state.data.getContent()) + ".json";
                state.data.getContent().add(new ContentAsset( assetName, ContentType.item, "{}"));
                state.data.reloadContent(false);
                state.data.regenerateContentSprites(false);
                Reflect.invoke(assetsDialog, "rebuild");
            });
        }});
    }

    private static String findAssetName(String base, Seq<? extends DataAsset> assets){
        ObjectSet<String> set = ObjectSet.with(assets.map(a -> a.name));
        for(int index = 0; ; index++){
            String name = base + index;
            if(set.add(name)) return name;
        }
    }
}
