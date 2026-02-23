package MinRi2.PatchEditor.export;

import MinRi2.PatchEditor.export.ObjectExporter.*;
import MinRi2.PatchEditor.node.*;
import arc.util.serialization.*;
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
        JsonValue json = ObjectExporter.exportObject(objectNode, config);
        return Jval.read(json.toJson(OutputType.json)).toString(Jformat.hjson);
    }
}
