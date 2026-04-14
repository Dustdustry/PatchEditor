package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.ui.dialog.*;
import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.JsonWriter.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.core.GameState.*;
import mindustry.gen.*;
import mindustry.mod.DataPatcher.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.state;

/**
 * @author minri2
 * Create by 2024/2/17
 */
public class PatchManager extends BaseDialog{
    private final PatchEditor editor = new PatchEditor();
    private final Table patchContainer, patchTable;
    private final Seq<EditorPatch> editorPatches = new Seq<>();
    private String searchText = "";
    private boolean sortByAlphabet;

    private static boolean initFavorite;

    public PatchManager(){
        super("");

        shouldPause = true;
        patchContainer = new Table();
        patchTable = new Table();
        resized(this::rebuildCont);

        shown(() -> {
            if(!initFavorite){
                initFavorite = true;
                FieldFavorites.init();
                FieldNotes.init();
            }
        });
        shown(() -> {
            editorPatches.set(state.patcher.patches.map(EditorPatch::new));
            Vars.state.patcher.unapply();

            // patcher will change the object so clear all the tree
            editor.resetEditor();

            setup();
            rebuildCont();
        });

        hidden(() -> {
            try{
                state.patcher.apply(editorPatches.map(p -> p.patch));
            }catch(Exception e){
                Vars.ui.showException(e);
            }
            editorPatches.set(state.patcher.patches.map(EditorPatch::new));

            editor.resetEditor();

            if(state.isGame()){
                try{
                    Vars.logic.update();
                }catch(Exception e){
                    state.set(State.paused);
                    Vars.ui.showException("@patch-editor.editInGame.error", e);
                }
            }
        });

        editor.hidden(this::rebuildPatchTable);
    }

    private void setup(){
        if(cont.hasChildren()) return;

        patchContainer.background(Tex.whiteui).setColor(EPalettes.main);

        cont.add(patchContainer);

        addCloseButton();
    }

    private void rebuildCont(){
        Table table = patchContainer;
        table.clearChildren();
        rebuildPatchTable();

        table.table(Styles.grayPanel, title -> {
            title.left();

            title.add("@patch-manager").pad(8f).expandX().left();
            title.button(b -> {
                b.image(Icon.settingsSmall).pad(4f);
                b.add("@settings");
            }, Styles.cleari, () -> {
                new EditorSettings().show();
            }).growY().padRight(4f);
        }).pad(8f).growX();

        table.row();

        table.table(Styles.grayPanel, main -> {
            main.collapser(this::setupSearchTable, () -> editorPatches.size >= 8).padLeft(8f).padRight(8f).growX();

            main.row();

            main.pane(Styles.noBarPane, patchTable).scrollX(false).maxHeight(64f * 6).pad(8f).grow();
        }).pad(8f).grow();

        table.row();

        table.table(Styles.grayPanel, buttonTable -> {
            buttonTable.defaults().minWidth(130f).height(40f).margin(8f).pad(8f).growX();

            buttonTable.button("@patch-manager.add-patch", Icon.add, Styles.cleart, () -> {
                String name = findPatchName();
                JsonValue json = new JsonValue(ValueType.object);
                json.addChild("name", new JsonValue(name));
                editorPatches.add(new EditorPatch(name, json.toJson(OutputType.json)));
                rebuildPatchTable();
            });

            buttonTable.button("@patch-manager.import-patch", Icon.add, Styles.cleart, () -> {
                String text = Core.app.getClipboardText();

                JsonValue value;
                try{
                    value = PatchJsonIO.getParser().getJson().fromJson(null, Jval.read(text).toString(Jformat.plain));
                }catch(Exception ignored){
                    EUI.infoToast("@patch-manager.import-patch.failed");
                    return;
                }

                JsonValue nameValue = value.get("name");
                String name = nameValue != null && nameValue.isString() ? nameValue.asString() : findPatchName();
                editorPatches.add(new EditorPatch(name, text));
                rebuildPatchTable();

                EUI.infoToast("@patch-manager.import-patch.succeed");
            }).disabled(b -> Core.app.getClipboardText() == null);
        }).pad(8f).padTop(4f).growX();
    }

    private void rebuildPatchTable(){
        patchTable.clearChildren();

        Seq<EditorPatch> showPatches = editorPatches.select(p -> Strings.matches(searchText, p.displayName()));

        if(sortByAlphabet){
            showPatches.sort(Structs.comparing(p -> p.name));
        }

        int index = 0;
        for(EditorPatch patch : showPatches){
            patchTable.table(Tex.whiteui, t -> {
                t.add(patch.displayName()).labelAlign(Align.center).minWidth(32f).pad(4f).growX();

                t.table(buttons -> {
                    buttons.defaults().size(32f).pad(4f);

                    buttons.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                        Vars.ui.showConfirm("@confirm", Core.bundle.format("patch-manager.patch.remove.confirm", patch.displayName()), () -> {
                            editorPatches.remove(patch, true);
                            rebuildPatchTable();
                        });
                    }).tooltip("@patch-manager.patch.remove", true);

                    buttons.button(Icon.copySmall, Styles.clearNonei, () -> {
                        Core.app.setClipboardText(patch.patch);
                        EUI.infoToast("[green]Copy: []" + patch.name);
                    }).tooltip("@patch-manager.patch.copy", true);

                    buttons.button(Icon.editSmall, Styles.clearNonei, () -> {
                        editor.edit(patch);
                    }).tooltip("@patch-manager.patch.edit", true);
                }).pad(4f);

                t.image().width(4f).color(Color.darkGray).growY().right();
                t.row();
                Cell<?> horizontalLine = t.image().height(4f).color(Color.darkGray).growX();
                horizontalLine.colspan(t.getColumns());
            }).pad(8f).color(EPalettes.gray);

            if(++index % 3 == 0){
                patchTable.row();
            }
        }
    }

    private void setupSearchTable(Table table){
        table.image(Icon.zoomSmall).size(Vars.iconSmall);

        TextField field = table.add(EUI.deboundTextField(searchText, text -> {
            searchText = text;
            rebuildPatchTable();
        })).padLeft(4f).padRight(4f).growX().get();

        if(Core.app.isDesktop()){
            Core.scene.setKeyboardFocus(field);
        }

        table.image().color(Color.darkGray).pad(4f).width(2f).growY();

        table.button(Icon.upOpenSmall, Styles.clearNoneTogglei, () -> {
            sortByAlphabet = !sortByAlphabet;
            rebuildPatchTable();
        }).checked(b -> sortByAlphabet).tooltip("@patch-manager.sort.alphabet").width(Vars.iconSmall).growY();

        table.button(Icon.cancelSmall, Styles.clearNonei, () -> {
            searchText = "";
            field.setText(searchText);
            rebuildPatchTable();
        }).disabled(b -> searchText.isEmpty()).width(Vars.iconSmall).growY();
    }

    private String findPatchName(){
        String base = "Patch";

        int index = 0;
        while(true){
            String name = base + index;
            if(editorPatches.contains(p -> name.equals(p.name))){
                index++;
            }else{
                return name;
            }
        }
    }

    public static class EditorPatch{
        public String name;
        public String patch;

        public EditorPatch(String name, String patch){
            this.name = name;
            this.patch = patch;
        }

        public EditorPatch(PatchSet patchSet){
            this(patchSet.name, patchSet.patch);
        }

        public String displayName(){
            return name == null || name.isEmpty() ? "<unnamed>" : name;
        }
    }
}
