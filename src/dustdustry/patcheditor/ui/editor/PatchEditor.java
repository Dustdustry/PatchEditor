package dustdustry.patcheditor.ui.editor;

import dustdustry.patcheditor.node.*;
import dustdustry.patcheditor.node.patch.*;
import dustdustry.patcheditor.ui.*;
import dustdustry.patcheditor.ui.dialog.*;
import dustdustry.patcheditor.ui.editor.PatchManager.*;
import arc.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
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
    private ScrollPane pane;

    private EditorPatch editPatch;

    /**
     * <p>
     * ObjectTree、EditorTree、PatchTree
     * 分别用来描述 Mindustry的内容数据、编辑器的ui数据、内容包的json数据
     * </p>
     * <p>
     * EditorTree根据 ObjectTree 静态创建，根据 PatchTree 动态创建(DynamicEditorNode)
     * EditorNode根据 PatchNode 描述ui的状态（是否有patch，是否是追加，类型是否改变等等）
     * 并且暴露的各种通过 PatchOperator操作 PatchNode 的函数（比如：创建PatchNode，删除PatchNode等等）
     * </p>
     * PatchTree根据用户的 json 解析产生
     * PatchNode只存储json数据，比如 json类型(object, array)，或者值
     * <p>
     * 用户操作的逻辑链路:
     * 1. 导入json --解析--> PatchTree （解析：转为json树、根据ObjectTree脱糖）
     * 2. PatchTree + ObjectTree --构建--> EditorTree
     * 3. EditorNode -对应-> Modifier -> ModifierBuilder -> 构建ui
     * 4. 用户ui操作 -> Modifier -> 调用 EditorNode 的操作函数 -> Operator 传给 manager 应用并记录操作 -> 重新构建受影响的 EditorNode
     * 5. Modifier根据修改后 PatchNode 的状态，更新ui
     * </p>
     * <p>
     * 由于需要单值修改的 ObjectNode 很多，而且不同类型有不同的ui构建方式、修改PatchNode的方式，于是有 EditorNode 和 PatchNode 的中间层 DataModifier和ModifierBuilder
     * <p>
     * DataModifier 有自己的 Builder （比如：String类型会映射到 StringModifier，决定 PatchNode 的类型为值类型，即 key: value，有StringBuilder构建ui为输入条）
     * Builder 负责处理数据合法性，同时 DataModifier 还作为 Builer 的 consumer，提供修改的合法检查，提供初始值和是否修改
     * </p>
     * */
    private EditorNode editorTree;
    private ObjectNode objectTree;
    private final NodeManager manager;

    public PatchEditor(){
        super("@patch-editor");

        manager = new NodeManager();
        card = new NodeCard(manager);

        // notify here?
        manager.onChanged((operator, node, uiUpdated) -> {
            if(editorTree == null) return;

            editorTree.navigateThrough(operator.path, EditorNode::patchChanged);
        });

        resized(this::rebuild);
        shown(() -> {
            setup();
            rebuild();
        });
        hidden(() -> {
            manager.clearStacks();
            PatchNode namePatch = manager.getRoot().getOrNull("name");
            editPatch.patch = PatchJsonIO.toPatch(objectTree, manager.getRoot(), EditorSettings.getPatchExportOptions());
            editPatch.name = namePatch != null && namePatch.value != null ? namePatch.value : "";

            pane = null;
        });

        update(() -> {
            if(Core.scene.getDialog() == this
            && Core.scene.getKeyboardFocus() != null
            && !Core.scene.getKeyboardFocus().isDescendantOf(this)){
                requestKeyboard();
            }
        });

        keyDown(KeyCode.up, card::extract);
        keyDown(KeyCode.down, card::editLastData);

        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(button == KeyCode.mouseForward) card.editLastData();
                else if(button == KeyCode.mouseBack) card.extract();
                return super.touchDown(event, x, y, pointer, button);
            }
        });

        addCloseListener();

        update(() -> {
            if(Core.scene.getDialog() != this) return;
            if(Core.scene.getKeyboardFocus() != null && !Core.scene.getKeyboardFocus().isDescendantOf(this)) return;

            if(Core.input.ctrl()){
                if(Core.input.keyTap(KeyCode.z)){
                    if(Core.input.shift()){
                        manager.redo();
                    }else{
                        manager.undo();
                    }
                }else if(Core.input.keyTap(KeyCode.y)){
                    manager.redo();
                }
            }
        });
    }

    public void resetEditor(){
        manager.reset();
        ObjectResolver.clearTemplate();
        objectTree = ObjectNode.createRoot();
        editorTree = new EditorNode(objectTree, manager);
        card.setRootEditorNode(editorTree);
        card.clean();
    }

    public void edit(EditorPatch patch){
        manager.reset();
        editorTree = new EditorNode(objectTree, manager);
        card.setRootEditorNode(editorTree);

        try{
            PatchJsonIO.parseJson(objectTree, manager.getRoot(), patch.patch);
            manager.indexPaths();
        }catch(Exception e){
            Vars.ui.showException(e);
            return;
        }

        card.setEditPath("");

        editPatch = patch;

        show();
    }

    protected void setup(){
        if(cont.hasChildren()) return;

        titleTable.clearChildren();
        titleTable.left().background(Tex.whiteui).setColor(EPalettes.main);

        titleTable.addChild(title);
        title.setFillParent(true);
        title.setStyle(Styles.outlineLabel);

        titleTable.table(buttons -> {
            buttons.defaults().size(150f, 48f).pad(8f).growY();

            buttons.button("@quit", Icon.cancel, Styles.grayt, this::hide);
            buttons.button("@patch-editor.undo", Icon.undo, Styles.grayt, manager::undo)
            .disabled(b -> !manager.canUndo());
            buttons.button("@patch-editor.redo", Icon.redo, Styles.grayt, manager::redo)
            .disabled(b -> !manager.canRedo());
            if(Vars.mobile) buttons.button("@node-card.expandLast", Icon.downOpen, Styles.grayt, card::editLastData);

            buttons.add().expandX();
            buttons.button("@patch-editor.favorites", Icon.star, Styles.grayt, () -> EUI.favorites.show());
            buttons.button("@patch-editor.notes", Icon.book, Styles.grayt, () -> EUI.notes.show());

            for(Element child : buttons.getChildren()){
                if(child instanceof TextButton textButton){
                    for(Cell<?> cell : textButton.getCells()){
                        cell.pad(8f);
                    }
                }
            }
        }).growX();

        cont.top();
        addCloseListener();
    }

    protected void rebuild(){
        cont.clearChildren();

        card.rebuild();

        if(pane == null) pane = new ScrollPane(card, Styles.noBarPane);
        cont.add(pane).scrollX(false).pad(16f).padTop(8f).grow();
    }
}
