package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.node.patch.PatchOperator.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class EditorNode{
    private String path;
    private boolean resolvedObj;

    private ObjectNode shadowObjectNode;
    private final ObjectNode objectNode;

    private EditorNode parent;

    private ObjectNode currentObj;
    private final OrderedMap<String, EditorNode> children = new OrderedMap<>();

    private final NodeManager manager;

    public EditorNode(ObjectNode objectNode, NodeManager manager){
        this.objectNode = objectNode;
        this.manager = manager;
    }

    public String name(){
        return objectNode.name;
    }

    public ObjectMap<String, EditorNode> getChildren(){
        PatchNode patchNode = getPatch();

        ObjectNode objectNode = getObjNode();
        if(currentObj != objectNode){
            children.clear();
            resolvedObj = false;
            currentObj = objectNode;
        }

        if(!resolvedObj){
            for(ObjectNode node : objectNode.getChildren().values()){
                if(node.isSign()) continue;

                EditorNode child = new EditorNode(node, manager);
                child.parent = this;
                children.put(child.name(), child);
            }
            resolvedObj = true;
        }

        var iterator = children.entries().iterator();
        while(iterator.hasNext()){
            EditorNode node = iterator.next().value;
            if(node instanceof DynamicEditorNode){
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

                    try{
                        Class<?> type = PatchJsonIO.resolveType(getObjNode().elementType, typeJson);

                        EditorNode child = new DynamicEditorNode(childPatchNode.key, getObjNode().elementType, type, manager);
                        child.parent = this;
                        children.put(childPatchNode.key, child);
                    }catch(Exception e){
                        Log.err(e);
                    }
                }
            }
        }

        return children;
    }

    public ObjectNode getObjNode(){
        Class<?> type = objectNode.type;
        PatchNode patchNode = getPatch();
        if(patchNode != null){
            PatchNode typePatch = patchNode.getOrNull("type");
            if(typePatch != null && typePatch.value != null){
                type = PatchJsonIO.resolveType(objectNode.elementType, typePatch.value);
            }
        }

        if(type != objectNode.type){
            if(shadowObjectNode == null || shadowObjectNode.type != type){
                shadowObjectNode = ObjectResolver.getTemplate(objectNode.type, type);
            }
            return shadowObjectNode;
        }
        return objectNode;
    }

    public Object getObject(){
        return getObjNode().object;
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
        ObjectNode objectNode = getObjNode();
        if(objectNode.object == null) return objectNode.type;
        if(objectNode.object instanceof MapEntry<?,?> entry) return ClassHelper.unoymousClass(entry.value.getClass());
        return ClassHelper.unoymousClass(objectNode.object.getClass());
    }

    public boolean isChangedType(){
        PatchNode patchNode = getPatch();
        if(patchNode == null) return false;

        PatchNode typePatch = patchNode.getOrNull("type");
        return typePatch != null && typePatch.value != null && PatchJsonIO.resolveType(objectNode.elementType, typePatch.value) != null;
    }

    public String getPath(){
        if(path == null){
            path = parent == null ? ""
            : (!parent.getPath().isEmpty() ? (parent.getPath() + NodeManager.pathComp + name()) : name());
        }
        return path;
    }

    public PatchNode getPatch(){
        return manager.getPatch(getPath());
    }

    public EditorNode navigate(String path){
        if(path.isEmpty()) return this;

        EditorNode current = this;
        for(String name : path.split(NodeManager.pathSplitter)){
            current = current.getChildren().get(name);
            if(current == null) return null;
        }
        return current;
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

    public void putKey(String key){
        manager.applyOp(new MapPutOp(getPath(), key));
    }

    public void changeType(Class<?> type){
        manager.applyOp(new ChangeTypeOp(getPath(), type));
    }

    public void clearType(){
        manager.applyOp(new ClearOp(getPath()));
    }

    public static class DynamicEditorNode extends EditorNode{
        public final String key;

        public DynamicEditorNode(String key, Class<?> baseType, Class<?> type, NodeManager manager){
            super(new ObjectNode(key, ObjectResolver.getExample(baseType, type), baseType), manager);

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