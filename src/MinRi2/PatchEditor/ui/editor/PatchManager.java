package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.JsonWriter.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
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

    public PatchManager(){
        super("");

        patchContainer = new Table();
        patchTable = new Table();
        resized(this::rebuildCont);
        shown(() -> {
            editorPatches.set(state.patcher.patches.map(EditorPatch::new));
            Vars.state.patcher.unapply();
            // patcher will change the object so clear all the tree
            editor.clearTree();

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
        });

        editor.hidden(this::rebuildPatchTable);
    }

    private void setup(){
        if(cont.hasChildren()) return;

        patchContainer.background(Tex.whiteui).setColor(EPalettes.main);
        patchTable.background(Styles.grayPanel);

        cont.add(patchContainer);

        addCloseButton();
    }

    private void rebuildCont(){
        Table table = patchContainer;
        table.clearChildren();

        table.table(Styles.grayPanel, title -> {
            title.add("@patch-manager").pad(8f).expandX().left();
        }).pad(8f).growX();

        table.row();

        rebuildPatchTable();
        table.pane(patchTable).scrollY(false).pad(8f).grow();

        table.row();

        table.table(Styles.grayPanel, buttonTable -> {
            buttonTable.defaults().minWidth(130f).height(40f).margin(8f).pad(8f).growX();

            buttonTable.button("@add-patch", Icon.add, Styles.cleart, () -> {
                String name = findPatchName();
                JsonValue json = new JsonValue(ValueType.object);
                json.addChild("name", new JsonValue(name));
                editorPatches.add(new EditorPatch(name, json.toJson(OutputType.json)));
                rebuildPatchTable();
            });

            buttonTable.button("@import-patch", Icon.add, Styles.cleart, () -> {
                String text = Core.app.getClipboardText();

                JsonValue value;
                try{
                    value = PatchJsonIO.getParser().getJson().fromJson(null, Jval.read(text).toString(Jformat.plain));
                }catch(Exception ignored){
                    EUI.infoToast("@import-patch.failed");
                    return;
                }

                JsonValue nameValue = value.get("name");
                String name = nameValue != null && nameValue.isString() ? nameValue.asString() : findPatchName();
                editorPatches.add(new EditorPatch(name, text));
                rebuildPatchTable();

                EUI.infoToast("@import-patch.succeed");
            }).disabled(b -> Core.app.getClipboardText() != null && Core.app.getClipboardText().isEmpty());
        }).pad(8f).padTop(4f).growX();
    }

    private void rebuildPatchTable(){
        patchTable.clearChildren();

        int index = 0;
        for(EditorPatch patch : editorPatches){
            patchTable.table(Tex.whiteui, t -> {
                t.add(patch.name.isEmpty() ? "<unnamed>" : patch.name).labelAlign(Align.center).minWidth(32f).pad(4f).growX();

                t.table(buttons -> {
                    buttons.defaults().size(32f).pad(4f);

                    buttons.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                        editorPatches.remove(patch, true);
                        rebuildPatchTable();
                    }).tooltip("@patch.remove", true);

                    buttons.button(Icon.copySmall, Styles.clearNonei, () -> {
                        Core.app.setClipboardText(patch.patch);
                        EUI.infoToast("[green]Copy: []" + patch.name);
                    }).tooltip("@patch.copy", true);

                    buttons.button(Icon.editSmall, Styles.clearNonei, () -> {
                        editor.edit(patch);
                    }).tooltip("@patch.edit", true);
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
    }
}
