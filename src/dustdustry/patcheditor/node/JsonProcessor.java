package dustdustry.patcheditor.node;

import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import arc.util.serialization.JsonWriter.*;
import dustdustry.patcheditor.node.patch.*;
import dustdustry.patcheditor.utils.*;

public class JsonProcessor{
    public boolean sugarStacks;
    public boolean simplifyPath;
    public boolean formatJson;
    public OutputFormat format;

    private final ObjectNode objectNode;
    private final PatchNode patchNode;
    private final JsonValue jsonValue;

    public JsonProcessor(ObjectNode objectNode, PatchNode patchNode){
        this.objectNode = objectNode;
        this.patchNode = patchNode;
        this.jsonValue = null;
    }

    public JsonProcessor(ObjectNode objectNode, JsonValue jsonValue){
        this.objectNode = objectNode;
        this.jsonValue = jsonValue;
        this.patchNode = null;
    }

    public JsonProcessor sugarStacks(boolean sugarStacks){
        this.sugarStacks = sugarStacks;
        return this;
    }

    public JsonProcessor simplifyPath(boolean simplifyPath){
        this.simplifyPath = simplifyPath;
        return this;
    }

    public JsonProcessor formatJson(boolean formatJson){
        this.formatJson = formatJson;
        return this;
    }

    public JsonProcessor format(OutputFormat format){
        this.format = format;
        return this;
    }

    public JsonProcessor options(PatchExportOptions options){
        this.sugarStacks = options.sugarStacks;
        this.simplifyPath = options.simplifyPath;
        this.formatJson = options.formatJson;
        this.format = options.format;
        return this;
    }

    public String toJson(){
        JsonValue value = resolveValue();
        if(value == null) return "";
        return formatString(value);
    }

    public String toPatch(){
        JsonValue value = resolveValue();
        if(value == null) return "";

        SugarJsonConfig config = new SugarJsonConfig().sugarStacks(sugarStacks);
        JsonTransform.sugarPatch(objectNode, value, config);
        JsonTransform.processJson(objectNode, value, true);

        if(simplifyPath) JsonTransform.simplifyPath(value);

        return formatString(value);
    }

    public String toModJson(){
        JsonValue value = resolveValue();
        if(value == null) return "";

        SugarJsonConfig config = new SugarJsonConfig().sugarStacks(sugarStacks);
        JsonTransform.sugarPatch(objectNode, value, config);
        JsonTransform.processJson(objectNode, value, false);

        return formatString(value);
    }

    private JsonValue resolveValue(){
        if(jsonValue != null) return jsonValue;
        if(patchNode != null) return JsonTransform.toJsonValue(patchNode);
        return null;
    }

    private String formatString(JsonValue value){
        OutputFormat fmt = format != null ? format : OutputFormat.hjson;
        if(fmt == OutputFormat.hjson){
            return JsonValues.valueToJval(value).toString(Jformat.hjson);
        }else{
            return formatJson ? JsonValues.valueToJval(value).toString(Jformat.formatted) : value.toJson(OutputType.json);
        }
    }

    public static class SugarJsonConfig{
        public boolean sugarStacks = true;
        public SugarJsonConfig sugarStacks(boolean sugarStacks){
            this.sugarStacks = sugarStacks;
            return this;
        }
    }

    public enum OutputFormat{
        hjson, json
    }
}
