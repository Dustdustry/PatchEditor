package dustdustry.patcheditor.core;

import arc.util.serialization.*;
import arc.util.serialization.Jval;

public class JsonValues{

    public static Jval valueToJval(JsonValue value){
        return switch(value.type()){
            case stringValue -> Jval.valueOf(value.asString());
            case doubleValue -> Jval.valueOf(value.asDouble());
            case longValue -> Jval.valueOf(value.asLong());
            case booleanValue -> Jval.valueOf(value.asBoolean());
            case nullValue -> Jval.valueOf(null);
            case object -> {
                Jval obj = Jval.newObject();
                for(JsonValue childValue : value){
                    obj.put(childValue.name, valueToJval(childValue));
                }
                yield obj;
            }
            case array -> {
                Jval arr = Jval.newArray();
                for(JsonValue childValue : value){
                    arr.add(valueToJval(childValue));
                }
                yield arr;
            }
        };
    }

    public static void addFront(JsonValue parent, JsonValue value){
        parent.size++;
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

        if(parent != null) parent.size--;
    }

    public static void moveChild(JsonValue source, JsonValue target){
        JsonValue child = source.child;
        if(child == null) return;

        int sourceSize = source.size;

        source.child = null;
        target.child = child;
        JsonValue next = child;
        while(next != null){
            next.parent = target;
            next = next.next;
        }

        source.size = 0;
        target.size += sourceSize;
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
