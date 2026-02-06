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

    /**
     * ObjectTree、EditorTree、PatchTree
     * 分别用来描述 Mindustry的内容数据、编辑器的ui数据、内容包的json数据
     * <p>
     * EditorTree有 ObjectTree 静态部分和 PatchTree 动态部分(DynamicEditorNode)
     * ui通过 EditorNode 暴露的各种helper函数，调用 PatchOperator 来操作 PatchTree（比如：创建PatchNode，删除PatchNode等等）
     * <p>
     * PatchOperator 只存储一些修改 PatchTree 必要的数据，比如 json类型(object, array)，或者 设置json的值
     * <p>
     * 需要值修改的 ObjectNode 类型多，而且不同类型有不同的ui构建方式，以及修改PatchNode的方式，于是有 EditorNode 和 PatchNode 的中间层 DataModifier和ModifierBuilder
     * <p>
     * DataModifier 会有自己的 Builder （比如：String类型会映射到 StringModifier，决定 PatchNode 的类型为值类型，即 key: value，有StringBuilder构建ui为输入条）
     * Builder 负责处理数据合法性，同时 DataModifier 还作为 Builer 的 consumer，提供修改的合法检查，提供初始值和是否修改
     * */
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
            editPatch.patch = toPatch(objectTree, manager.getRoot());
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

    public static String toPatch(ObjectNode objectNode, PatchNode patchNode){
        JsonValue value = PatchJsonIO.toPatchJson(objectNode, patchNode);

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
