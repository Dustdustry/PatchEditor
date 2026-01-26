package MinRi2.PatchEditor.node.patch;

import MinRi2.PatchEditor.node.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.JsonValue.*;

import java.util.*;

public class PatchNode{
    public String key;
    public @Nullable String value;
    public ValueType type = ValueType.object;

    private PatchNode parent;
    public final ObjectMap<String, PatchNode> children = new OrderedMap<>();

    public @Nullable ModifierSign sign;

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

    public void clearChildren(){
        Iterator<PatchNode> iterator = children.values();
        while(iterator.hasNext()){
            PatchNode child = iterator.next();
            child.parent = null;
            iterator.remove();
        }
    }

    public void remainBySign(ModifierSign sign){
        Iterator<PatchNode> iterator = children.values();
        while(iterator.hasNext()){
            PatchNode child = iterator.next();
            if(child.sign != sign){
                child.parent = null;
                iterator.remove();
            }
        }
    }

    public PatchNode getParent(){
        return parent;
    }

    public String getPath(){
        if(parent == null) return "";
        String parentPath = parent.getPath();
        return (parentPath.isEmpty() ? "" : parentPath + ".") + key;
    }

    public PatchNode navigateChild(String path){
        return navigateChild(path, false);
    }

    public PatchNode navigateChild(String path, boolean create){
        if(path.isEmpty()) return this;

        PatchNode current = this;
        for(String name : path.split(NodeManager.pathSplitter)){
            current = create ? current.getOrCreate(name) : current.getOrNull(name);
            if(current == null) return null;
        }
        return current;
    }

    @Override
    public String toString(){
        return "PatchNode{" +
        "key='" + key + '\'' +
        ", value='" + value + '\'' +
        ", sign=" + sign +
        '}';
    }
}
