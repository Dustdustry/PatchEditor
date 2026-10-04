package dustdustry.patcheditor.export;

import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.core.resolve.*;
import dustdustry.patcheditor.export.ObjectExporter.*;
import arc.struct.ObjectMap.*;

public class PatchExporter{
    public static String export(String path){
        return export(ObjectNode.createRoot(ObjectResolver.patch).navigate(path));
    }

    public static String export(ObjectNode objectNode){
        return export(objectNode, new ExportConfig(), PatchExportOptions.defaults());
    }

    public static String export(ObjectNode objectNode, ExportConfig config){
        return export(objectNode, config, PatchExportOptions.defaults());
    }

    public static String export(ObjectNode objectNode, ExportConfig config, PatchExportOptions options){
        JNode value = JNode.object();
        if(objectNode.getParent() != null && objectNode.getParent().isRoot()){
            for(Entry<String, ObjectNode> entry : objectNode.getChildren()){
                JNode childValue = JNode.object(); // Just a default type, change the value type later.

                value.addChild(entry.key, childValue);
                ObjectExporter.exportObject(entry.value, childValue, config);
            }
        }else{
            ObjectExporter.exportObject(objectNode, value, config);
        }
        return new JsonProcessor(objectNode, value).options(options).toPatch();
    }
}
