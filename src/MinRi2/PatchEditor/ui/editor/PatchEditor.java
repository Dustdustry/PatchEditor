package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.ui.editor.PatchManager.*;
import arc.*;
import arc.input.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonWriter.*;
import arc.util.serialization.Jval.*;
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

    private EditorNode editorTree;
    private ObjectNode objectTree;
    private final NodeManager manager;

    public PatchEditor(){
        super("@patch-editor");

        manager = new NodeManager();
        card = new NodeCard();

        resized(this::rebuild);
        shown(() -> {
            if(objectTree == null) objectTree = ObjectNode.createRoot();
            if(editorTree == null) editorTree = new EditorNode(objectTree, manager);
            card.setRootEditorNode(editorTree);
            card.setEditorNode("");

            setup();
            rebuild();
        });
        hidden(() -> {
            JsonValue value = PatchJsonIO.toJson(manager.getRoot());
            String minifyJson = PatchJsonIO.simplifyPatch(value).toJson(OutputType.json);
            editPatch.patch = Jval.read(minifyJson).toString(Jformat.hjson);

            card.setRootEditorNode(null);
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

    public void clearTree(){
        objectTree = null;
        editorTree = null;
    }

    public void edit(EditorPatch patch){
        manager.reset();

        try{
            PatchJsonIO.parseJson(objectTree, manager.getRoot(), patch.patch);
        }catch(Exception e){
            Vars.ui.showException(e);
            return;
        }

        editPatch = patch;

        show();
    }

    protected void setup(){
        if(cont.hasChildren()) return;

        titleTable.clearChildren();
        titleTable.background(Tex.whiteui).setColor(EPalettes.main);

        titleTable.table(buttons -> {
            buttons.defaults().size(150f, 64f).pad(8f).growY();

            buttons.button("@quit", Icon.cancel, Styles.grayt, this::hide);
            if(Vars.mobile) buttons.button("@node-card.expandLast", Icon.downOpen, Styles.grayt, () -> card.getFrontCard().editLastData());
        });

        titleTable.add(title).style(Styles.outlineLabel).growX();

        cont.top();
        addCloseListener();
    }

    protected void rebuild(){
        cont.clearChildren();

        card.rebuild();
        cont.pane(Styles.noBarPane, card).scrollX(false).pad(16f).padTop(8f).grow();
    }
}
