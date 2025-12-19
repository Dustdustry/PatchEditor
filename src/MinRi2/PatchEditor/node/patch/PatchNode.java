package MinRi2.PatchEditor.node.patch;

import MinRi2.PatchEditor.node.*;
import arc.struct.*;
import arc.util.*;

public class PatchNode{
    public String key;
    public @Nullable String value;
    private PatchNode parent;
    public final ObjectMap<String, PatchNode> children = new ObjectMap<>();

    // mark while operating or parsing
    public PatchType patchType;

    public PatchNode(String key){
        this(key, null);
    }

    public PatchNode(String key, String value){
        this.key = key;
        this.value = value;
    }

    public PatchNode getOrNull(String key){
        return children.get(key);
    }

    public PatchNode getOrCreate(String key){
        PatchNode child = children.get(key);
        if(child != null) return child;
        children.put(key, child = new PatchNode(key));
        child.parent = this;
        return child;
    }

    public void remove(){
        if(parent != null){
            parent.children.remove(key);
            children.clear();
            parent = null;
        }
    }

    public PatchNode getParent(){
        return parent;
    }

    public PatchNode navigateChild(String path){
        return navigateChild(path, false);
    }

    public PatchNode navigateChild(String path, boolean create){
        PatchNode current = this;
        for(String name : path.split(PatchNodeManager.pathSplitter)){
            current = create ? current.getOrCreate(name) : current.getOrNull(name);
            if(current == null) return null;
        }
        return current;
    }

    @Override
    public String toString(){
        return "PatchNode{" +
        "value='" + value + '\'' +
        ", key='" + key + '\'' +
        '}';
    }

    public enum PatchType{
        // used for Array, Seq, ObjectSet as field
        APPEND,
        // used for ObjectMap as the value
        REMOVE,
        // mark as modifiable and overrideable
        MODIFY;
    }
}
