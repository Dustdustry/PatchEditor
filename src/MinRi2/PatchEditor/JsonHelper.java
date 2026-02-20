package MinRi2.PatchEditor;

import arc.util.serialization.*;

public class JsonHelper{

    public static void addFront(JsonValue parent, JsonValue value){
        JsonValue child = parent.child;
        if(child == null){
            parent.child = value;
            value.parent = parent;
            return;
        }

        parent.child = value;
        child.prev = value;

        value.prev = null;
        value.next = child;
        value.parent = parent;
    }

    public static void remove(JsonValue value){
        JsonValue parent = value.parent, prev = value.prev, next = value.next;

        if(prev != null) prev.next = next;
        else if(parent != null) parent.child = next;

        if(next != null) next.prev = prev;
        value.parent = value.prev = value.next = null;
    }

    public static void moveChild(JsonValue source, JsonValue target){
        JsonValue child = source.child;
        if(child == null) return;

        source.child = null;
        target.child = child;
        JsonValue next = child;
        while(next != null){
            next.parent = target;
            next = next.next;
        }

    }

    public static void replace(JsonValue replaced, JsonValue value){
        if(value.parent != null) remove(value);

        JsonValue parent = replaced.parent, prev = replaced.prev, next = replaced.next;

        if(prev != null) prev.next = value;
        else if(parent != null) parent.child = value;

        if(next != null) next.prev = value;

        value.parent = parent;
        value.prev = prev;
        value.next = next;

        replaced.parent = replaced.prev = replaced.next = null;
    }
}
