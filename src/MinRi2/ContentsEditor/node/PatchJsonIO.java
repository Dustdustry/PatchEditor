package MinRi2.ContentsEditor.node;

import arc.util.*;
import arc.util.serialization.*;
import mindustry.ctype.*;

public class PatchJsonIO{

    public static Object readData(NodeData data){
        if(data.jsonData == null) return null;
        Class<?> type = NodeHelper.getType(data);
        if(type == null) return null;
        return NodeHelper.getParser().getJson().readValue(type, data.jsonData);
    }

    public static String getKeyName(Object object){
        if(object instanceof MappableContent mc) return mc.name;
        if(object instanceof Enum<?> e) return e.name();
        if(object instanceof Class<?> clazz) return clazz.getName();
        return String.valueOf(object);
    }
}