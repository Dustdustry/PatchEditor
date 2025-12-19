package MinRi2.PatchEditor.node;

import arc.util.serialization.*;

// A helper class. I want kt's extend function
public class PatchNode extends JsonValue{
    public PatchNode(String name, ValueType type){
        super(type);
        set(name);
    }

    public PatchNode getOrCreate(String name){
        return getOrCreate(name, ValueType.object);
    }

    public PatchNode getOrCreate(String name, ValueType type){
        PatchNode node = (PatchNode)get(name);
        if(node == null){
            addChild(name, node = new PatchNode(name, type));
        }
        return node;
    }

    public void setJson(JsonValue value){
        setName(value.name);
        setType(value.type());
        if(value.isValue()) set(value.asString());
    }

    public void remove(){
        remove(this);
    }

    public boolean isEmpty(){
        return !isValue() && child == null;
    }

    // TODO: bad place
    public static void remove(JsonValue value){
        JsonValue parent = value.parent, prev = value.prev, next = value.next;

        if(prev != null) prev.next = next;
        else if(parent != null) parent.child = next;

        if(next != null) next.prev = prev;
        value.parent = value.prev = value.next = null;

        if(parent != null && parent.child == null) remove(parent);
    }
}
