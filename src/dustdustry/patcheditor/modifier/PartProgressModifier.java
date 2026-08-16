package dustdustry.patcheditor.modifier;

import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.modifier.ModifierBuilder.*;
import mindustry.entities.part.DrawPart.*;

public class PartProgressModifier extends DataModifier<ProgressBuilder>{

    public PartProgressModifier(){
        builder = new ProgressUiBuilder(this);
    }

    @Override
    public ProgressBuilder readValue(EditorNode node){
        if(node.hasValue()){
            PatchNode patchNode = node.getPatch();
            if(patchNode != null) return PatchJsonIO.parseProgressBuilder(JsonTransform.toJsonValue(patchNode));
        }else if(node.getObject() instanceof PartProgress progress){
            return new ProgressBuilder(progress);
        }
        return null;
    }

    @Override
    public void writeValue(PatchNode patch, ProgressBuilder value){
        PatchJsonIO.toPatchNode(value, patch);
    }

    @Override
    public boolean isDefault(ProgressBuilder value, EditorNode node){
        PartProgress def = (PartProgress)node.getObject();
        return value.ops.isEmpty() && value.base.equals(def);
    }

}
