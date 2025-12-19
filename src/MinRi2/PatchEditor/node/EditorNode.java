package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.patch.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class EditorNode{
    private static EditorNode rootData;
    public final ObjectNode objectNode;
    private String path;

    private @Nullable EditorNode parent;
    private final OrderedMap<String, EditorNode> children = new OrderedMap<>();
    private boolean resolved;

    // just a reference
    private final PatchNodeManager manager;

    public EditorNode(ObjectNode objectNode, PatchNodeManager manager){
        this.objectNode = objectNode;
        this.manager = manager;
    }

    public static EditorNode getRootData(){
        return rootData;
    }

    public String name(){
        return objectNode.name;
    }

    public boolean isRoot(){
        return parent == null;
    }

    public EditorNode getSign(ModifierSign sign){
        return getChild(sign.sign);
    }

    public EditorNode getChild(String name){
         return getChildren().get(name);
    }

    public ObjectMap<String, EditorNode> getChildren(){
        if(!resolved){
            for(ObjectNode node : objectNode.getChildren().values()){
                EditorNode child = new EditorNode(node, manager);
                child.parent = this;
                children.put(child.name(), child);
            }
            resolved = true;
        }
        return children;
    }

    public EditorNode getParent(){
        return parent;
    }

    public Object getObject(){
        return objectNode.object;
    }

    public Object getDisplayValue(){
        PatchNode patchNode = getPatch();
        if(patchNode == null || patchNode.value == null) return getObject();
        JsonValue value = PatchJsonIO.toJson(patchNode, new JsonValue(ValueType.object));
        return PatchJsonIO.getParser().getJson().readValue(getTypeIn(), value);
    }

    public boolean hasValue(){
        return getPatch() != null;
    }

    public Class<?> getTypeIn(){
        return objectNode.type;
    }

    public Class<?> getTypeOut(){
        if(objectNode.object == null) return objectNode.type;
        if(objectNode.object instanceof MapEntry<?,?> entry) return ClassHelper.unoymousClass(entry.value.getClass());
        return ClassHelper.unoymousClass(objectNode.object.getClass());
    }

    public String getPath(){
        if(path == null){
            // Path is relative to root so don't append it
            boolean split = parent != null && !parent.isRoot() && !parent.getPath().isEmpty();
            path = split ? (parent.getPath() + PatchNodeManager.pathComp + name()) : name();
        }

        return path;
    }

    public PatchNode getPatch(){
        return manager.getPatch(getPath());
    }

    public void setValue(String value){
        manager.applyOp(new PatchOperator.SetOp(path, value));
    }

    public void clearJson(){
        manager.applyOp(new PatchOperator.ClearOp(path));
    }

    public static class PlusEditorNode extends EditorNode{

        public PlusEditorNode(ObjectNode objectNode, PatchNodeManager manager){
            super(objectNode, manager);
        }

        @Override
        public String name(){
            return ModifierSign.PLUS.sign;
        }

        @Override
        public boolean hasValue(){
            return true;
        }
    }

    public static class DynamicEditorNode extends EditorNode{
        public Class<?> type;
        public Object example;

        public DynamicEditorNode(ObjectNode objectNode, PatchNodeManager manager){
            super(objectNode, manager);
        }
    }
}