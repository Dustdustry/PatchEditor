package MinRi2.ContentsEditor.node;

import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;

public class PatchNode extends JsonValue{
    public PatchNode(String name, ValueType type){
        super(type);
        set(name);
    }

    public PatchNode getOrCreate(String name){
        return (PatchNode)get(name);
    }

    public void setJson(JsonValue value){
        setName(value.name);
        setType(value.type());
        if(value.isValue()) set(value.asString());
    }

    public void remove(){
        remove(this);
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
