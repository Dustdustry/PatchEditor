package MinRi2.PatchEditor;

import MinRi2.PatchEditor.node.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.type.*;

import java.lang.reflect.*;

public class ObjectExample{
    private static final ObjectMap<Class<?>, Object> exampleMap = new ObjectMap<>();

    public static Object getExample(Class<?> base, Class<?> type){
        if(type == float.class || type == Float.class) return 0f;
        if(type == double.class || type == Double.class) return 0d;
        if(type == boolean.class || type == Boolean.class) return false;
        if(type == short.class || type == Short.class) return (short)0;
        if(type == byte.class || type == Byte.class) return (byte)0;
        if(type == char.class || type == Character.class) return '\0';
        if(type.isArray()) return Array.newInstance(type.getComponentType(), 0);

        type = PatchJsonIO.resolveType(type);

        Object example = exampleMap.get(type);
        if(example != null) return example;

        base = PatchJsonIO.getTypeParser(base);
        if(MappableContent.class.isAssignableFrom(base)){
            example = getContentExample(base);
        }

        if(example == null){
            JsonValue value = new JsonValue(ValueType.object);
            value.addChild("type", new JsonValue(PatchJsonIO.getClassTypeName(type)));

            try{
                Json parserJson = PatchJsonIO.getParser().getJson();
                // Invoke internalRead to skip null fields checking.
                example = Reflect.invoke(parserJson, "internalRead", new Object[]{base, null, value, null}, Class.class, Class.class, JsonValue.class, Class.class);
            }catch(Exception ignored){
                return null;
            }
        }

        exampleMap.put(type, example);
        return example;
    }

    private static Object getContentExample(Class<?> type){
        MappableContent content;
        try{
            Constructor<?> cons = type.getConstructor(String.class);
            content = (MappableContent)cons.newInstance("patch-editor-example");
        }catch(Exception ignored){
            return null;
        }

        // remove from contentMap, contentNameMap and nameMap.
        Vars.content.getBy(content.getContentType()).remove(content, true);
        Vars.content.getNamesBy(content.getContentType()).remove(content.name);
        ObjectMap<Object, String> map = Reflect.get(Vars.content, "nameMap");
        map.remove(content.name);

        return content;
    }
}
