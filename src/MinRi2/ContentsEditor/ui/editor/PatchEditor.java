package MinRi2.ContentsEditor.ui.editor;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.ui.*;
import arc.*;
import arc.input.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonWriter.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.ContentPatcher.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class PatchEditor extends BaseDialog{
    private final NodeData rootData;
    private final NodeCard card;

    private PatchSet editPatch;

    public PatchEditor(){
        super("@contents-editor");

        rootData = NodeData.getRootData();
        card = new NodeCard();

        setup();

        resized(this::rebuild);
        shown(this::rebuild);
        hidden(() -> editPatch.patch = rootData.jsonData.toJson(OutputType.json));

        update(() -> {
            if(Core.scene.getDialog() == this
            && Core.scene.getKeyboardFocus() != null
            && !Core.scene.getKeyboardFocus().isDescendantOf(this)){
                requestKeyboard();
            }
        });

        keyDown(KeyCode.up, () -> {
            NodeCard front = card.getFrontCard();
            if(front != card){
                front.extractWorking();
            }
        });

        keyDown(KeyCode.down, () -> card.getFrontCard().editLastData());

        addCloseListener();
    }

    public void edit(PatchSet patch){
        editPatch = patch;

        rootData.clearJson();
        JsonValue value = NodeHelper.getParser().getJson().fromJson(null, Jval.read(patch.patch).toString(Jformat.plain));
        rootData.setJsonData(value);

        show();
    }

    protected void setup(){
        titleTable.clearChildren();
        titleTable.background(Tex.whiteui).setColor(EPalettes.main);

        titleTable.table(buttons -> {
            buttons.defaults().size(150f, 64f).pad(8f).growY();

            buttons.button("@quit", Icon.cancel, Styles.grayt, this::hide);
            if(Vars.mobile) buttons.button("@node-card.expandLast", Icon.downOpen, Styles.grayt, () -> card.getFrontCard().editLastData());
        });

        titleTable.add(title).style(Styles.outlineLabel).growX();

        cont.top();

        card.setNodeData(rootData);

        addCloseListener();
    }

    protected void rebuild(){
        cont.clearChildren();

        card.rebuild();
        cont.pane(Styles.noBarPane, card).scrollX(false).pad(16f).padTop(8f).grow();
    }
}
