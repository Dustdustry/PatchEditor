package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.node.patch.PatchOperator.*;
import MinRi2.PatchEditor.utils.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.JsonValue.*;
import mindustry.ctype.*;

import java.lang.reflect.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class EditorNode{
    private String path;
    private boolean needResolve = true;

    private ObjectNode currentObj;
    private final ObjectNode objectNode;

    private EditorNode parent;
    private final OrderedMap<String, EditorNode> children = new OrderedMap<>();

    private final NodeManager manager;
    private boolean patchChanged = true;

    public EditorNode(ObjectNode objectNode, NodeManager manager){
        this.objectNode = objectNode;
        this.manager = manager;

        currentObj = objectNode;
    }

    public String name(){
        return objectNode.name;
    }

    public ObjectMap<String, EditorNode> buildChildren(){
        PatchNode patchNode = getPatch();

        if(needResolve){
            needResolve = false;
            for(ObjectNode node : currentObj.getChildren().values()){
                if(node.isSign()) continue;

                EditorNode child = new EditorNode(node, manager);
                child.parent = this;
                children.put(child.name(), child);
            }
        }

        if(patchChanged){
            patchChanged = false;

            var iterator = children.entries().iterator();
            while(iterator.hasNext()){
                EditorNode childNode = iterator.next().value;
                if(childNode instanceof DynamicEditorNode || childNode instanceof InvalidEditorNode){
                    childNode.parent = null;
                    childNode.children.clear();
                    iterator.remove();
                }
            }

            // data driven
            if(patchNode != null){
                for(PatchNode childPatch : patchNode.children.values()){
                    if(childPatch.sign == ModifierSign.PLUS){
                        PatchNode typeNode = childPatch.getOrNull("type");
                        String typeJson = typeNode == null ? null : typeNode.value;

                        try{
                            // changing type support
                            Class<?> type = PatchJsonIO.resolveType(typeJson);
                            if(type == null) type = getObjNode().elementType; // Not changing type. Use meta type.

                            EditorNode child = new DynamicEditorNode(childPatch.key, getObjNode().elementType, type, manager);
                            child.parent = this;
                            children.put(child.name(), child);
                        }catch(Exception e){
                            Log.err(e);
                        }
                    }else if(!children.containsKey(childPatch.key)){
                        EditorNode child = new InvalidEditorNode(childPatch.key, manager);
                        child.parent = this;
                        children.put(childPatch.key, child);
                    }
                }
            }
        }

        return children;
    }

    public ObjectNode getObjNode(){
        return currentObj;
    }

    public ObjectNode getMetaNode(){
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
        Object object = getObject();
        if(patchNode == null || isRemoving()) return object;
        if(PatchJsonIO.overrideable(getTypeIn()) && !(isOverriding() || isAppended() || patchNode.value != null)) return object;
        return PatchJsonIO.parseJsonObject(patchNode, getObjNode(), getObject());
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

    public boolean isParentOverriding(){
        EditorNode current = parent;
        while(current != null){
            if(current.isOverriding()) return true;
            current = current.parent;
        }
        return false;
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

    public boolean isRequired(){
        if(getPatch() != null || getObjNode() == null) return false;
        Field field = getObjNode().field;
        if(field == null || field.getType().isPrimitive()) return false;
        if(MappableContent.class.isAssignableFrom(field.getType())){
            return !field.getType().isAnnotationPresent(Nullable.class) && getObject() == null;
        }

        return false;
    }

    public String getPath(){
        if(path == null){
            path = parent == null ? ""
            : (!parent.getPath().isEmpty() ? (parent.getPath() + NodeManager.pathComp + name()) : name());
        }
        return path;
    }

    public PatchNode getPatch(){
        return getPatch(false);
    }

    public PatchNode getPatch(boolean create){
        return manager.getPatch(getPath(), create);
    }

    public EditorNode navigate(String path){
        if(path == null || path.isEmpty()) return this;

        EditorNode current = this;
        int start = 0;
        while(true){
            int dot = path.indexOf(NodeManager.pathComp, start);
            String name = dot == -1 ? path.substring(start) : path.substring(start, dot);
            current = current.buildChildren().get(name);
            if(current == null || dot == -1) return current;
            start = dot + 1;
        }
    }

    public void navigateThrough(String path, Cons<EditorNode> cons){
        if(path == null || path.isEmpty()) return;

        EditorNode current = this;
        int start = 0;
        while(true){
            cons.get(current);

            int dot = path.indexOf(NodeManager.pathComp, start);
            String name = dot == -1 ? path.substring(start) : path.substring(start, dot);
            current = current.buildChildren().get(name);
            if(current == null) return;
            start = dot + 1;
        }
    }

    public void setValue(String value, ValueType valueType, boolean uiUpdated){
        manager.applyOp(new SetOp(getPath(), value, valueType), uiUpdated);
    }

    public void setValueType(ValueType type){
        manager.applyOp(new SetValueTypeOp(getPath(), type));
    }

    public void clearJson(){
        clearJson(false);
    }

    public void clearJson(boolean uiUpdated){
        manager.applyOp(new ClearOp(getPath()), uiUpdated);
    }

    public void append(boolean plusSyntax){
        manager.applyOp(new AppendOp(getPath(), objectNode.elementType, plusSyntax));
    }

    public void touch(String key, String value, ModifierSign sign){
        manager.applyOp(new TouchOp(getPath(), key, value, sign));
    }

    public void changeType(Class<?> type){
        clearChildren(); // resolve again
        manager.applyOp(new ChangeTypeOp(getPath(), type));
    }

    public void setSign(ModifierSign sign){
        if(sign == ModifierSign.MODIFY && ClassHelper.isArrayLike(getTypeIn())){
            manager.applyOp(new BatchOp(getPath(),
            new SetSignOp(getPath(), sign),
            new SetValueTypeOp(getPath(), ValueType.array),
            new ClearChildrenOp(getPath())
            ));
        }else{
            manager.applyOp(new SetSignOp(getPath(), sign));
        }
    }

    public void setRemoved(){
        manager.applyOp(new BatchOp(getPath(),
        new SetSignOp(getPath(), ModifierSign.REMOVE),
        new SetOp(getPath(), ModifierSign.REMOVE.sign, null)
        ));
    }

    public void importPatch(String patch){
        PatchNode sourceNode = new PatchNode(name());
        PatchJsonIO.parseJson(getObjNode(), sourceNode, patch);
        PatchJsonTransform.clearRedundantPatch(getObjNode(), sourceNode);
        manager.applyOp(new ImportOp(getPath(), sourceNode));
    }

    public void clearChildren(){
        children.clear();
        needResolve = true;
        patchChanged = true;
    }

    public void sync(){
        checkObjNode();
        if(needResolve || patchChanged){
            buildChildren();
        }
    }

    public void checkObjNode(){
        ObjectNode resolved = objectNode;

        Class<?> type = getTypeIn();
        if(isOverriding() && ClassHelper.isContainer(type)){
            resolved = ObjectResolver.getShadowNode(objectNode, type);
        }else{
            PatchNode patchNode = getPatch();
            if(patchNode != null){
                PatchNode typePatch = patchNode.getOrNull("type");
                if(typePatch != null && typePatch.value != null){
                    type = PatchJsonIO.resolveType(typePatch.value);
                }

                if(type != null && PatchJsonIO.typeOverrideable(type)){
                    if(type != currentObj.type){
                        resolved = ObjectResolver.getShadowNode(objectNode, type);
                    }
                }
            }
        }

        if(currentObj != resolved){
            currentObj = resolved;
            clearChildren();
        }
    }

    public void patchChanged(){
        patchChanged = true;
        checkObjNode();
    }

    @Override
    public String toString(){
        return "EditorNode{" +
        "path='" + getPath() + '\'' +
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
        public Class<?> getTypeIn(){
            return baseType;
        }

        @Override
        public boolean isAppended(){
            return true;
        }
    }

    public static class InvalidEditorNode extends EditorNode{
        public final String key;

        public InvalidEditorNode(String key, NodeManager manager){
            super(ObjectResolver.getTemplate(Object.class), manager);
            this.key = key;
        }

        @Override
        public String name(){
            return key;
        }

        @Override
        public boolean hasValue(){
            return true;
        }
    }
}
