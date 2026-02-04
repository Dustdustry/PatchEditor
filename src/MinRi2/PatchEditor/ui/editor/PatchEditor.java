package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.ui.editor.EditorSettings.*;
import MinRi2.PatchEditor.ui.editor.PatchManager.*;
import arc.*;
import arc.input.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
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
            setup();
            rebuild();
        });
        hidden(() -> {
            editPatch.patch = toPatch(manager.getRoot());
            // clear the root node reference
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
            if(front != card) front.extractWorking();
        });
        keyDown(KeyCode.down, () -> card.getFrontCard().editLastData());

        addCloseListener();

        update(() -> {
           if(Core.input.keyTap(KeyCode.tab)){
               if(!Core.input.shift()){
                   card.getFrontCard().editLastData();
               }else{
                   NodeCard front = card.getFrontCard();
                   if(front != card) front.extractWorking();
               }
           }
        });
    }

    public void edit(EditorPatch patch){
        // patcher will change the object so clear all the tree
        manager.reset();
        objectTree = ObjectNode.createRoot();
        editorTree = new EditorNode(objectTree, manager);

        try{
            PatchJsonIO.parseJson(objectTree, manager.getRoot(), patch.patch);
        }catch(Exception e){
            Vars.ui.showException(e);
            return;
        }

        card.setRootEditorNode(editorTree);
        card.setEditPath("");

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

    public static String toPatch(PatchNode patchNode){
        JsonValue value = PatchJsonIO.toJson(patchNode);

        if(Core.settings.getBool("patch-editor.simplifyPatch")){
            PatchJsonIO.simplifyPatch(value);
        }

        String exportType = Core.settings.getString("patch-editor.exportType");
        if(ExportType.hjson.name().equals(exportType)){
            return Jval.read(value.toJson(OutputType.json)).toString(Jformat.hjson);
        }else{
            return value.toJson(OutputType.json);
        }
    }
}
