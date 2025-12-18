package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.node.*;
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
    private final NodeData rootData;
    private final NodeCard card;

    private EditorPatch editPatch;

    public PatchEditor(){
        super("@contents-editor");

        rootData = NodeData.getRootData();
        card = new NodeCard();

        setup();

        resized(this::rebuild);
        shown(this::rebuild);
        hidden(() -> {
            JsonValue data = PatchJsonIO.toJson(rootData);
            editPatch.patch = PatchJsonIO.simplifyPatch(data).toJson(OutputType.json);
            rootData.clearJson();
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
        try{
            PatchJsonIO.parseJson(rootData, patch.patch);
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

        card.setData(rootData);

        addCloseListener();
    }

    protected void rebuild(){
        cont.clearChildren();

        card.rebuild();
        cont.pane(Styles.noBarPane, card).scrollX(false).pad(16f).padTop(8f).grow();
    }
}
