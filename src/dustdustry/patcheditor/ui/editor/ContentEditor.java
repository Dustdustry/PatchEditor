package dustdustry.patcheditor.ui.editor;

import arc.scene.ui.layout.*;
import dustdustry.patcheditor.node.*;
import dustdustry.patcheditor.node.patch.PatchOperator.*;
import dustdustry.patcheditor.node.resolve.*;
import dustdustry.patcheditor.ui.*;
import dustdustry.patcheditor.ui.dialog.*;
import dustdustry.patcheditor.ui.editor.PatchManager.*;
import dustdustry.patcheditor.utils.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.mod.data.*;
import mindustry.ui.*;

import static mindustry.Vars.state;

public class ContentEditor extends PatchEditor{
    protected ContentAsset asset;

    public ContentEditor(){
        super();

        title.setText("@content-editor");

        card.forceOverride(true);
        hidden(this::resetEditor);
    }

    @Override
    protected void savePatch(){
        String patch = PatchJsonIO.toPatch(objectTree, manager.getRoot(), EditorSettings.getPatchExportOptions());
        asset.data = PatchJsonTransform.toModJson(patch);
        state.data.reloadContent(false);
        state.data.regenerateContentSprites(false);
    }

    @Override
    public void resetEditor(){
        manager.reset();
        ObjectResolver.clearTemplate();
        objectTree = null;
        editorTree = null;
        card.setRootEditorNode(null);
        card.clean();

        asset = null;
        editPatch = null;
    }

    @Override
    protected void setupTinyButton(Table table){
        super.setupTinyButton(table);

        table.button(Icon.wrench, Styles.cleari, () -> {
            if(asset.type == ContentType.block){
                EUI.blockClassSelector.select(clazz -> {
                    manager.applyOp(new ChangeTypeOp("", clazz));
                    savePatch();
                    edit(asset, onSaved);
                    return true;
                });
            }else{
                EUI.classSelector.select(asset.type.contentClass, (clazz) -> {
                    manager.applyOp(new ChangeTypeOp("", clazz));
                    savePatch();
                    edit(asset, onSaved);
                    return true;
                });
            }
        }).size(50f).pad(4f).tooltip("@node.changeType", true);
    }

    @Deprecated
    @Override
    public void edit(EditorPatch patch){
        throw new RuntimeException("Deprecated");
    }

    public void edit(ContentAsset asset){
        edit(asset, null);
    }

    public void edit(ContentAsset asset, Runnable onSaved){
        this.asset = asset;
        editPatch = new EditorPatch(asset.name, asset.data);

        Content content = asset.content;
        ContentType type = asset.type;

        if(content == null){
            objectTree = ObjectResolver.getTemplate(type.contentClass, ObjectResolver.content);
        }else{
            Class<?> typeContent = ClassHelper.unoymousClass(content.getClass());
            String name = content instanceof MappableContent mc ? mc.name : "";
            objectTree = new ObjectNode(name, ObjectExample.getExample(typeContent, typeContent, true), typeContent);
            objectTree.strategy = ObjectResolver.content;
        }

        super.edit(editPatch, onSaved);
    }
}
