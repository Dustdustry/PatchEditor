package dustdustry.patcheditor.utils;

import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
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
}
