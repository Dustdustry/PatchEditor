package MinRi2.ContentsEditor.node;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.JsonValue.*;

/**
 * Store the json nodes with untree-structured data.
 * @author minri2
 * Create by 2024/2/15
 */
public class EditorNode{
    private static EditorNode rootData;

    public final ObjectNode objectNode;
    public PatchNode patchNode;

    private @Nullable EditorNode parent;
    private final OrderedMap<String, EditorNode> children = new OrderedMap<>();
    private boolean resolved;

    public EditorNode(ObjectNode objectNode){
        this(objectNode, null);
    }

    public EditorNode(ObjectNode objectNode, PatchNode patchNode){
        this.objectNode = objectNode;
        this.patchNode = patchNode;
    }

    public static EditorNode getRootData(){
        if(rootData == null){
            rootData = new EditorNode(ObjectNode.getRoot());
            rootData.patchNode = new PatchNode("root", ValueType.object);
        }
        return rootData;
    }

    public String name(){
        return objectNode.name;
    }

    public EditorNode getSign(ModifierSign sign){
        return getChild(sign.sign);
    }

    public EditorNode getChild(String name){
         return getChildren().get(name);
    }

    public ObjectMap<String, EditorNode> getChildren(){
        if(!resolved){
            for(ObjectNode node : objectNode.getChildren()){
                EditorNode child = new EditorNode(node);
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
        return getObject();
    }

    public Seq<String> getPath(){
        Seq<String> path = new Seq<>();
        EditorNode current = this;
        while(current != rootData){
            path.add(current.name());
            current = current.parent;
        }
        return path.reverse();
    }

    public boolean hasValue(){
        return patchNode != null;
    }

    public void setValue(String value){
        if(patchNode == null){
            Seq<String> path = getPath();

            PatchNode currentPatch = getRootData().patchNode;
            for(String pathName : path){
                currentPatch = currentPatch.getOrCreate(pathName);
            }

            patchNode = currentPatch;
        }

        patchNode.value = value;
    }

    public boolean isRoot(){
        return this == rootData;
    }

    public void initJson(){
        if(patchNode != null) return;

        if(parent != null && parent.patchNode == null) parent.initJson();
    }

    public void clearJson(){
        patchNode = null;

        cleanPatchNode();

        EditorNode current = parent;
        while(current != null && current.patchNode.children.isEmpty()){
            current.patchNode = null;
            current = current.parent;
        }
    }

    public void cleanPatchNode(){
        if(patchNode != null && parent != null && parent.patchNode == null){
            patchNode = null;
        }

        for(EditorNode child : children.values()){
            child.cleanPatchNode();
        }
    }

    public static class PlusEditorNode extends EditorNode{

        public PlusEditorNode(ObjectNode objectNode, PatchNode patchNode){
            super(objectNode, patchNode);
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

        public DynamicEditorNode(ObjectNode objectNode, PatchNode patchNode){
            super(objectNode, patchNode);
        }
    }
}