package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.modifier.*;
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
    private ObjectNode objectNode;
    private String path;

    private @Nullable EditorNode parent;
    private final OrderedMap<String, EditorNode> children = new OrderedMap<>();

    private final NodeManager manager;

    public EditorNode(ObjectNode objectNode, NodeManager manager){
        this.objectNode = objectNode;
        this.manager = manager;
    }

    public String name(){
        return objectNode.name;
    }

    public boolean isRoot(){
        return parent == null;
    }

    public ObjectMap<String, EditorNode> getChildren(){
        children.clear();

        PatchNode patchNode = getPatch();

        for(ObjectNode node : objectNode.getChildren().values()){
            if(node.isSign()) continue;

            EditorNode child = new EditorNode(node, manager);
            child.parent = this;
            children.put(child.name(), child);
        }

        // data driven
        if(patchNode != null){
            for(PatchNode childPatchNode : patchNode.children.values()){
                if(childPatchNode.sign == ModifierSign.PLUS){
                    PatchNode typeNode = patchNode.getOrNull("type");
                    String typeJson = typeNode == null ? null : typeNode.value;

                    Class<?> type = PatchJsonIO.resolveType(getObjNode().elementType, typeJson);

                    String key = childPatchNode.key;
                    if(ClassHelper.isArrayLike(getTypeIn())) key = PatchJsonIO.getContainerSize(getObject()) + "";
                    EditorNode child = new DynamicEditorNode(key, type, manager);
                    child.parent = this;
                    children.put(key, child);
                }
            }
        }
        return children;
    }

    public EditorNode getParent(){
        return parent;
    }

    public ObjectNode getObjNode(){
        return objectNode;
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
            path = split ? (parent.getPath() + NodeManager.pathComp + name()) : name();
        }

        return path;
    }

    protected void setObjectNode(ObjectNode newObj){
        objectNode = newObj;
        children.clear();
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

    public static class DynamicEditorNode extends EditorNode{
        public final String key;
        private Class<?> type;

        public DynamicEditorNode(String key, Class<?> type, NodeManager manager){
            super(new ObjectNode(key, NodeModifier.getExample(type, type), type), manager);

            this.key = key;
            this.type = type;
        }

        @Override
        public boolean hasValue(){
            return true;
        }

        public void changeType(Class<?> newType){
            type = newType;
            setObjectNode(new ObjectNode(key, NodeModifier.getExample(type, type), type));
        }

        @Override
        public String name(){
            return key;
        }
    }
}