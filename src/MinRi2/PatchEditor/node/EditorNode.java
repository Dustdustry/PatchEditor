package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.node.patch.PatchOperator.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.mod.*;

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
    private boolean dynamicDirty = true;

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

        boolean isOverriding = isOverriding();
        if(isOverriding){
            clearChildren();
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

        if(dynamicDirty){
            dynamicDirty = false;

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
                            Class<?> type = PatchJsonIO.resolveType(typeJson);
                            if(type == null) continue; // type invalid

                            EditorNode child = new DynamicEditorNode(childPatchNode.key, getObjNode().elementType, type, manager);
                            child.parent = this;
                            children.put(child.name(), child);
                        }catch(Exception e){
                            Log.err(e);
                        }
                    }
                }
            }
        }

        return children;
    }

    public ObjectNode getObjNode(){
        if(!PatchJsonIO.typeOverrideable(getTypeIn())) return objectNode;

        Class<?> type = objectNode.type;
        PatchNode patchNode = getPatch();
        if(patchNode != null){
            PatchNode typePatch = patchNode.getOrNull("type");
            if(typePatch != null && typePatch.value != null){
                type = PatchJsonIO.resolveType(typePatch.value);
                if(type == null) return objectNode; // type invalid
            }
        }

        if(type != objectNode.type){
            if(shadowObjectNode == null || shadowObjectNode.type != type){
                shadowObjectNode = ObjectResolver.getShadowNode(objectNode, type);
            }
            return shadowObjectNode;
        }
        return objectNode;
    }

    public Object getObject(){
        Object object = getObjNode().object;
        if(object instanceof MapEntry<?,?> entry) return entry.value;
        return object;
    }

    public String getDisplayName(){
        return name();
    }

    public Object getDisplayValue(){
        PatchNode patchNode = getPatch();
        if(patchNode == null || isRemoving()) return getObject();
        // Parse array or map may cause lag. Using original object is ok.
        if(ClassHelper.isArrayLike(getTypeIn()) || ClassHelper.isMap(getTypeIn())) return getObject();
        Json json = PatchJsonIO.getParser().getJson();
        try{
            JsonValue value = PatchJsonIO.toJson(patchNode);
            if(patchNode.value != null) return json.readValue(getTypeIn(), value);

            // TODO: cache?
            Object copied = PatchJsonIO.cloneObject(getObject());
            if(copied == null) return json.readValue(getTypeIn(), value);

            // stimulate patch applying
            json.readFields(copied, value);
            return copied;
        }catch(Exception e){
            // may expect class value
            if(patchNode.value != null){
                Class<?> type = ClassMap.classes.get(patchNode.value);
                if(type != null) return type;
            }
            return getObject();
        }
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

    public boolean isAppended(){
        return false;
    }

    public boolean isRemoving(){
        PatchNode patchNode = getPatch();
        return patchNode != null && ModifierSign.REMOVE.sign.equals(patchNode.value);
    }

    public boolean isOverriding(){
        PatchNode patchNode = getPatch();
        return patchNode != null && patchNode.sign == ModifierSign.MODIFY;
    }

    public boolean isChangedType(){
        if(!PatchJsonIO.typeOverrideable(getTypeIn())) return false;

        PatchNode patchNode = getPatch();
        if(patchNode == null) return false;

        PatchNode typePatch = patchNode.getOrNull("type");
        return typePatch != null && typePatch.value != null && PatchJsonIO.resolveType(typePatch.value) != null;
    }

    public boolean isEditable(){
        return parent != null && !isRemoving();
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
        if(path == null || path.isEmpty()) return this;

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
        dynamicChanged();
        manager.applyOp(new ClearOp(getPath()));
    }

    public void append(boolean appendPrefix){
        dynamicChanged();
        manager.applyOp(new ArrayAppendOp(getPath(), appendPrefix));
    }

    public void putKey(String key){
        dynamicChanged();
        manager.applyOp(new MapPutOp(getPath(), key));
    }

    public void changeType(Class<?> type){
        clearChildren(); // resolve again
        dynamicChanged();
        manager.applyOp(new ChangeTypeOp(getPath(), type));
    }

    public void setSign(ModifierSign sign){
        dynamicChanged();
        manager.applyOp(new SetSignOp(getPath(), sign));
        if(ClassHelper.isArrayLike(getTypeIn())){
            manager.applyOp(new SetValueTypeOp(getPath(), ValueType.array));
        }
    }

    public void clearChildren(){
        children.clear();
        resolvedObj = false;
        dynamicDirty = true;
    }

    public void dynamicChanged(){
        dynamicDirty = true;
    }

    @Override
    public String toString(){
        return "EditorNode{" +
        "path='" + path + '\'' +
        '}';
    }

    public static class DynamicEditorNode extends EditorNode{
        public final String key;
        public final Class<?> baseType;

        public DynamicEditorNode(String key, Class<?> baseType, Class<?> type, NodeManager manager){
            super(ObjectResolver.getTemplate(type), manager);

            this.key = key;
            this.baseType = baseType;
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

        @Override
        public Class<?> getTypeIn(){
            return baseType;
        }

        @Override
        public boolean isAppended(){
            return true;
        }
    }
}