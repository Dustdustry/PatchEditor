package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.node.patch.PatchOperator.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.mod.*;
import mindustry.type.*;

import java.time.format.*;

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
            children.clear();
            resolvedObj = false;
            dynamicDirty = true;
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
                            Class<?> type = PatchJsonIO.resolveType(getObjNode().elementType, typeJson);

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
        Class<?> type = objectNode.type;
        PatchNode patchNode = getPatch();
        if(patchNode != null){
            PatchNode typePatch = patchNode.getOrNull("type");
            // UnitType's type have its resolver.
            if(typePatch != null && typePatch.value != null && !UnitType.class.isAssignableFrom(type)){
                type = PatchJsonIO.resolveType(objectNode.elementType, typePatch.value);
            }
        }

        if(type != objectNode.type || isOverriding()){
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
        if(patchNode == null || patchNode.value == null) return getObject();
        if(isRemoving()) return getObject();
        try{
            JsonValue value = PatchJsonIO.toJson(patchNode);
            return PatchJsonIO.getParser().getJson().readValue(getTypeIn(), value);
        }catch(Exception e){
            // may expect class value
            Class<?> type = ClassMap.classes.get(patchNode.value);
            if(type != null) return type;
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

    public boolean isAppending(){
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
        PatchNode patchNode = getPatch();
        if(patchNode == null) return false;

        // UnitType's type have its resolver.
        if(UnitType.class.isAssignableFrom(getTypeIn())) return false;

        PatchNode typePatch = patchNode.getOrNull("type");
        return typePatch != null && typePatch.value != null
        && PatchJsonIO.resolveType(objectNode.elementType, typePatch.value) != null;
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

    public void dynamicChanged(){
        dynamicDirty = true;
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
        public boolean isAppending(){
            return true;
        }
    }
}