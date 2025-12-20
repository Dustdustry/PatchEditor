package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.modifier.*;
import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.node.patch.PatchOperator.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;

import java.util.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class EditorNode{
    private final ObjectNode objectNode;
    private final String path;

    private boolean resolvedObj;
    private @Nullable EditorNode parent;
    private final OrderedMap<String, EditorNode> children = new OrderedMap<>();

    private final NodeManager manager;

    public EditorNode(EditorNode parent, ObjectNode objectNode, NodeManager manager){
        this.objectNode = objectNode;
        this.manager = manager;

        this.parent = parent;

        if(parent == null){
            path = "";
        }else{
            path = !parent.getPath().isEmpty() ? (parent.getPath() + NodeManager.pathComp + name()) : name();
        }
    }

    public String name(){
        return objectNode.name;
    }

    public ObjectMap<String, EditorNode> getChildren(){
        PatchNode patchNode = getPatch();

        if(!resolvedObj){
            for(ObjectNode node : objectNode.getChildren().values()){
                if(node.isSign()) continue;

                EditorNode child = new EditorNode(this, node, manager);
                children.put(child.name(), child);
            }
            resolvedObj = true;
        }

        var iterator = children.entries().iterator();
        while(iterator.hasNext()){
            EditorNode node = iterator.next().value;
            if(node instanceof DynamicEditorNode){
                node.parent = null;
                node.children.clear();
                iterator.remove();
            }
        }

        // data driven
        if(patchNode != null){
            for(PatchNode childPatchNode : patchNode.children.values()){
                if(childPatchNode.sign == ModifierSign.PLUS){
                    PatchNode typeNode = childPatchNode.getOrNull("type");
                    String typeJson = typeNode == null ? null : typeNode.value;

                    Class<?> type = PatchJsonIO.resolveType(getObjNode().elementType, typeJson);

                    EditorNode child = new DynamicEditorNode(this, childPatchNode.key, getObjNode().elementType, type, manager);
                    child.parent = this;
                    children.put(childPatchNode.key, child);
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

    public String getDisplayName(){
        return name();
    }

    public Object getDisplayValue(){
        PatchNode patchNode = getPatch();
        if(patchNode == null || patchNode.value == null) return getObject();
        JsonValue value = PatchJsonIO.toJson(patchNode);
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
        return path;
    }

    public PatchNode getPatch(){
        return manager.getPatch(getPath());
    }

    public void setValue(String value){
        manager.applyOp(new SetOp(getPath(), value));
    }

    public void clearJson(){
        manager.applyOp(new ClearOp(getPath()));
    }

    public void append(){
        manager.applyOp(new ArrayAppendOp(getPath()));
    }

    public void changeType(Class<?> type){
        manager.applyOp(new ChangeTypeOp(getPath(), type));
    }

    public static class DynamicEditorNode extends EditorNode{
        public final String key;

        public DynamicEditorNode(EditorNode parent, String key, Class<?> baseType, Class<?> type, NodeManager manager){
            super(parent, new ObjectNode(key, NodeModifier.getExample(baseType, type), baseType), manager);

            this.key = key;
        }

        @Override
        public boolean hasValue(){
            return true;
        }

        @Override
        public String name(){
            return key;
        }

        @Override
        public String getDisplayName(){
            return key;
        }
    }
}