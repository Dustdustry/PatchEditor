package dustdustry.patcheditor.core;

import dustdustry.patcheditor.core.resolve.*;
import dustdustry.patcheditor.core.PatchOperator.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Jval.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.consumers.*;

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

    private boolean objNodeInvalid = true;
    private Class<?> shadowType;

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
        ObjectNode current = getObjNode();
        PatchNode patchNode = getPatch();

        if(needResolve){
            needResolve = false;
            for(ObjectNode node : current.getChildren().values()){
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
                    boolean elemInvalid = false;
                    if(childPatch.sign == ModifierSign.PLUS){
                        PatchNode typeNode = childPatch.getOrNull("type");
                        String typeJson = typeNode == null ? null : typeNode.value;

                        try{
                            // changing type support
                            Class<?> type = PatchJsonIO.resolveType(typeJson);
                            if(type == null) type = current.elementType; // Not changing type. Use meta type.
                            if(current.elementType == null) throw new RuntimeException("Unknown elementType of '" + getPath() + "'");

                            EditorNode child = new DynamicEditorNode(childPatch.key, current.elementType, type, manager, current.getResolutionStrategy());
                            child.parent = this;
                            children.put(child.name(), child);
                        }catch(Exception e){
                            Log.err(e);
                            elemInvalid = true;
                        }

                        if(!elemInvalid) continue;
                    }

                    if(elemInvalid || isInvalidNode(children, childPatch)){
                        EditorNode child = new InvalidEditorNode(childPatch.key, manager, current.getResolutionStrategy());
                        child.parent = this;
                        children.put(childPatch.key, child);
                    }
                }
            }
        }

        return children;
    }

    protected boolean isInvalidNode(OrderedMap<String, EditorNode> children, PatchNode childPatch){
        String key = childPatch.key;
        if(children.containsKey(key) || "type".equals(key)) return false;
        return !UnitType.class.isAssignableFrom(getTypeIn()) || !"template".equals(key);
    }

    public ObjectNode getObjNode(){
        if(objNodeInvalid) resolveObjNode();
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
        Object parsed = PatchJsonIO.parseJsonObject(patchNode, getObjNode(), getObject());
        return parsed == null ? getObject() : parsed;
    }

    public boolean hasValue(){
        return manager.hasPatch(getPath());
    }

    public Class<?> getTypeIn(){
        return objectNode.type;
    }

    public Class<?> getTypeOut(){
        ObjectNode objectNode = getObjNode();
        if(objectNode.object == null) return objectNode.type;
        if(objectNode.object instanceof MapEntry<?,?> entry) return ClassHelper.actualClass(entry.value.getClass());
        return ClassHelper.actualClass(objectNode.object.getClass());
    }

    public boolean canAppend(){
        if(getObject() == null) return true;
        if(!ClassHelper.isArrayLike(getTypeIn()) || isAppended() || isOverriding()) return false;

        ObjectNode current = getObjNode();
        while(current != null){
            // Append to array of consume is illegal.
            if("consumes".equals(current.name) && current.type == Consume.class){
                return false;
            }
            current = current.getParent();
        }

        return true;
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

    public boolean isObjectForm(){
        PatchNode patchNode = getPatch();
        if(patchNode == null) return false;
        if(isChangedType()) return true;
        if(isOverriding() && ClassHelper.isContainer(getTypeIn())) return true;
        if(patchNode.value != null) return false;
        return patchNode.children.size > 0;
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

    public @Nullable String getFieldId(){
        if(objectNode == null || objectNode.field == null) return null;
        return PatchJsonIO.getTypeName(objectNode.field.getDeclaringClass()) + "#" + objectNode.field.getName();
    }

    public String parentPath(){
        return parent.getPath();
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
        for(String s : path.split(NodeManager.pathSplitter)){
            current = current.buildChildren().get(s);
            if(current == null) return current;
        }
        return current;
    }

    public void navigateThrough(String path, Cons<EditorNode> cons){
        cons.get(this);

        if(path == null || path.isEmpty()) return;

        EditorNode current = this;
        for(String s : path.split(NodeManager.pathSplitter)){
            current = current.buildChildren().get(s);
            if(current == null) return;
            cons.get(current);
        }
    }

    public Seq<EditorNode> navigateThrough(String path, Seq<EditorNode> seq){
        navigateThrough(path, seq::add);
        return seq;
    }

    public void setValue(String value, Jtype valueType, boolean uiUpdated){
        manager.applyOp(new SetOp(getPath(), value, valueType), uiUpdated);
    }

    public void setValueType(Jtype type){
        manager.applyOp(new SetValueTypeOp(getPath(), type));
    }

    public void clearJson(){
        clearJson(false);
    }

    public void clearJson(boolean uiUpdated){
        clearChildren();
        manager.applyOp(new ClearOp(getPath()), uiUpdated);
    }

    public void append(boolean plusSyntax){
        manager.applyOp(new AppendOp(getPath(), objectNode.elementType, plusSyntax, objectNode.getResolutionStrategy()));
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
            new SetValueTypeOp(getPath(), Jtype.array),
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
        JsonTransform.clearRedundant(getObjNode(), sourceNode);
        manager.applyOp(new ImportOp(getPath(), sourceNode));
    }

    public void appendElements(String patch){
        if(!ClassHelper.isArrayLike(getTypeIn())) return;

        Seq<PatchNode> elements = PatchJsonIO.parseElements(getObjNode(), patch);
        if(elements.isEmpty()) return;

        manager.applyOp(new AppendElementsOp(getPath(), elements, objectNode.elementType, canAppend(), objectNode.getResolutionStrategy()));
    }

    public void setPatch(PatchNode source, boolean uiUpdated){
        manager.applyOp(new BatchOp(getPath(),
            new ClearChildrenOp(getPath()),
            new ImportOp(getPath(), source)
        ), uiUpdated);
    }

    public void clearChildren(){
        // help gc
        for(EditorNode child : children.values()){
            child.parent = null;
            child.children.clear();
        }
        children.clear();
        needResolve = true;
        patchChanged = true;
    }

    public void sync(){
        getObjNode(); // resolves the type; a type change invalidates children
        if(needResolve || patchChanged){
            buildChildren();
        }
    }

    protected void resolveObjNode(){
        objNodeInvalid = false;

        Class<?> type = computeShadowType();
        if(type == shadowType) return;

        ObjectNode next = type == null ? objectNode : ObjectResolver.getShadowNode(objectNode, type);

        shadowType = type;
        currentObj = next;
        clearChildren();
    }

    // Returns the type of the shadow node to display, or null to keep the object.
    protected @Nullable Class<?> computeShadowType(){
        PatchNode patchNode = getPatch();
        if(patchNode == null) return null;

        Class<?> declared = getTypeIn();

        // Literally change the type
        PatchNode typePatch = patchNode.getOrNull("type");
        if(typePatch != null && typePatch.value != null){
            Class<?> parsed = PatchJsonIO.resolveType(typePatch.value);
            if(parsed == null) return null;
            // appended element: the node is already a template of this type
            if(parsed == declared && patchNode.sign == ModifierSign.PLUS) return null;
            // even changing back to the declared type rebuilds the value
            return PatchJsonIO.typeOverrideable(parsed) ? parsed : null;
        }

        if(patchNode.sign != ModifierSign.MODIFY || patchNode.value != null) return null;

        if(ClassHelper.isContainer(declared)) return declared;

        if(!PatchJsonIO.typeOverrideable(declared)) return null;

        // object override, e.g. bullet: {}: rebuilt as the declared type,
        // or its default subclass (Ability -> ForceFieldAbility)
        Class<?> rebuiltType = PatchJsonIO.resolveType(declared);
        if(ClassHelper.isAbstractClass(rebuiltType)) return null;

        Object object = objectNode.object;
        if(object == null) return rebuiltType;

        return ClassHelper.actualClass(object.getClass()) == ClassHelper.actualClass(rebuiltType) ? null : rebuiltType;
    }

    public void patchChanged(){
        patchChanged = true;
        objNodeInvalid = true;
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

        public DynamicEditorNode(String key, Class<?> baseType, Class<?> type, NodeManager manager, ResolutionStrategy strategy){
            super(ObjectResolver.getTemplate(type, strategy), manager);

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

        public InvalidEditorNode(String key, NodeManager manager, ResolutionStrategy strategy){
            super(ObjectResolver.getTemplate(Object.class, strategy), manager);
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
