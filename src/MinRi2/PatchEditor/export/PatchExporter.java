package MinRi2.PatchEditor.export;

import MinRi2.PatchEditor.export.ObjectExporter.*;
import MinRi2.PatchEditor.node.*;
import arc.struct.ObjectMap.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.JsonWriter.*;
import arc.util.serialization.Jval.*;

public class PatchExporter{
    public static String export(String path){
        return export(ObjectNode.createRoot().navigate(path));
    }

    public static String export(ObjectNode objectNode){
        return export(objectNode, new ExportConfig());
    }

    public static String export(ObjectNode objectNode, ExportConfig config){
        JsonValue value = new JsonValue(ValueType.object);
        if(objectNode.getParent() != null && objectNode.getParent().isRoot()){
            for(Entry<String, ObjectNode> entry : objectNode.getChildren()){
                JsonValue childValue = new JsonValue(ValueType.object);

                value.addChild(entry.key, childValue);
                ObjectExporter.exportObject(entry.value, childValue, config);
            }
        }else{
            ObjectExporter.exportObject(objectNode, value, config);
        }
        return PatchJsonIO.toPatch(objectNode, value);
    }
}
