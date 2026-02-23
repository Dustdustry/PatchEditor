package MinRi2.PatchEditor.export;

import MinRi2.PatchEditor.export.ObjectExporter.*;
import MinRi2.PatchEditor.node.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonWriter.*;
import arc.util.serialization.Jval.*;

public class PatchExporter{
    public static String exportObject(ObjectNode objectNode){
        return exportObject(objectNode, new ExportConfig());
    }

    public static String exportObject(ObjectNode objectNode, ExportConfig config){
        JsonValue json = ObjectExporter.exportObject(objectNode, config);
        return Jval.read(json.toJson(OutputType.json)).toString(Jformat.hjson);
    }
}
