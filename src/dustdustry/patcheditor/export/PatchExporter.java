package dustdustry.patcheditor.export;

import dustdustry.patcheditor.export.ObjectExporter.*;
import dustdustry.patcheditor.node.*;
import arc.struct.ObjectMap.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;

public class PatchExporter{
    public static String export(String path){
        return export(ObjectNode.createRoot().navigate(path));
    }

    public static String export(ObjectNode objectNode){
        return export(objectNode, new ExportConfig(), PatchExportOptions.defaults());
    }

    public static String export(ObjectNode objectNode, ExportConfig config){
        return export(objectNode, config, PatchExportOptions.defaults());
    }

    public static String export(ObjectNode objectNode, ExportConfig config, PatchExportOptions options){
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
        return PatchJsonIO.toPatch(objectNode, value, options);
    }
}
