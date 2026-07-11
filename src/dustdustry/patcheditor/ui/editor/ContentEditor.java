package dustdustry.patcheditor.ui.editor;

import arc.func.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonWriter.*;
import dustdustry.patcheditor.*;
import dustdustry.patcheditor.node.*;
import dustdustry.patcheditor.node.PatchJsonTransform.*;
import dustdustry.patcheditor.node.patch.*;
import dustdustry.patcheditor.ui.editor.PatchManager.*;
import dustdustry.patcheditor.utils.*;
import mindustry.ctype.*;
import mindustry.mod.data.*;

public class ContentEditor extends PatchEditor{
    protected ContentAsset asset;

    public ContentEditor(){
        super();

        card.forceOverride(true);
        hidden(this::resetEditor);
    }

    @Override
    protected void savePatch(){
        JsonValue value = PatchJsonIO.toJson(manager.getRoot());
        SugarJsonConfig sugarJsonConfig = new SugarJsonConfig().sugarStacks(true);
        PatchJsonTransform.sugarPatch(objectTree, value, sugarJsonConfig);
        PatchJsonTransform.processJson(objectTree, value);
        PatchJsonTransform.simplifyPath(value);

        asset.data = value.prettyPrint(OutputType.json, 4);
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
            objectTree = ObjectResolver.getTemplate(type.contentClass);
        }else{
            Class<?> typeContent = ClassHelper.unoymousClass(content.getClass());
            String name = content instanceof MappableContent mc ? mc.name : "";
            objectTree = new ObjectNode(name, ObjectExample.getExample(typeContent), typeContent);
        }

        super.edit(editPatch, onSaved);
    }
}
