package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.ui.editor.PatchManager.*;
import arc.*;
import arc.input.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonWriter.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class PatchEditor extends BaseDialog{
    private final NodeCard card;

    private EditorPatch editPatch;

    private final EditorNode rootData;
    private final NodeManager manager;

    public PatchEditor(){
        super("@contents-editor");

        manager = new NodeManager();
        rootData = new EditorNode(ObjectNode.getRoot(), manager);
        card = new NodeCard(rootData);

        setup();

        resized(this::rebuild);
        shown(this::rebuild);
        hidden(() -> {
            JsonValue value = PatchJsonIO.toPatchJson(ObjectNode.getRoot(), manager.getRoot());
            editPatch.patch = PatchJsonIO.simplifyPatch(value).toJson(OutputType.json);
        });

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

    public void edit(EditorPatch patch){
        manager.reset();

        try{
            PatchJsonIO.parseJson(rootData.getObjNode(), manager.getRoot(), patch.patch);
        }catch(Exception e){
            Vars.ui.showException(e);
            return;
        }

        editPatch = patch;

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

        card.setEditorNode(rootData.getPath());

        addCloseListener();
    }

    protected void rebuild(){
        cont.clearChildren();

        card.rebuild();
        cont.pane(Styles.noBarPane, card).scrollX(false).pad(16f).padTop(8f).grow();
    }
}
