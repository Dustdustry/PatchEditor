package MinRi2.ContentsEditor.ui.editor;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.ui.*;
import arc.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.ContentPatcher.*;
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
    private Seq<PatchSet> patches;

    public PatchManager(){
        super("");

        patchContainer = new Table();
        patchTable = new Table();

        hidden(this::savePatch);
        resized(this::rebuildCont);
        shown(() -> {
            patches = state.patcher.patches;
            if(!cont.hasChildren()) setup();
            rebuildCont();
        });

        editor.hidden(this::savePatch);
    }

    private void setup(){
        titleTable.clearChildren();
        cont.clearChildren();

        patchContainer.background(Tex.whiteui).setColor(EPalettes.purpleAccent1);
        patchTable.background(Styles.grayPanel);

        cont.add(patchContainer);

        addCloseButton();
    }

    private void savePatch(){
        try{
            state.patcher.apply(patches.map(p -> p.patch));
        }catch(Exception e){
            Vars.ui.showException(e);
        }
        rebuildPatchTable();
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
            buttonTable.defaults().minWidth(130f).height(40f).margin(8f).growX();

            buttonTable.button("@add-patch", Icon.add, Styles.cleart, () -> {
                patches.add(new PatchSet("name: " + findPatchName(), new JsonValue("error")));
                savePatch();
            });

            buttonTable.button("@import-patch", Icon.add, Styles.cleart, () -> {
                String text = Core.app.getClipboardText();

                try{
                    JsonValue value = NodeHelper.getParser().getJson().fromJson(null, Jval.read(text).toString(Jformat.plain));
                    patches.add(new PatchSet(text, value));
                    savePatch();

                    EUI.infoToast("@import-patch.succeed");
                }catch(Exception ignored){
                    EUI.infoToast("@import-patch.failed");
                }
            }).disabled(b -> Core.app.getClipboardText() != null && Core.app.getClipboardText().isEmpty());
        }).pad(8f).padTop(4f).growX();
    }

    private void rebuildPatchTable(){
        patchTable.clearChildren();

        int index = 0;
        for(PatchSet patch : patches){
            patchTable.table(Tex.whiteui, t -> {
                t.add(patch.name.isEmpty() ? "<unnamed>" : patch.name).growX();

                t.table(buttons -> {
                    buttons.defaults().size(32f).pad(4f);

                    buttons.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                        patches.remove(patch);
                        savePatch();
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

        int[] index = {0};
        while(patches.contains(p -> p.name.equals(base + index[0]))){
            index[0]++;
        }
        return base + index[0];
    }
}
