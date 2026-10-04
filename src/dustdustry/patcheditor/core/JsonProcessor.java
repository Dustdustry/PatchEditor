package dustdustry.patcheditor.core;

import arc.util.serialization.*;
import arc.util.serialization.Jval.*;

public class JsonProcessor{
    public boolean sugarStacks;
    public boolean simplifyPath;
    public boolean formatJson;
    public OutputFormat format;

    private final ObjectNode objectNode;
    private final PatchNode patchNode;
    private final JNode jsonNode;

    public JsonProcessor(ObjectNode objectNode, PatchNode patchNode){
        this.objectNode = objectNode;
        this.patchNode = patchNode;
        this.jsonNode = null;
    }

    public JsonProcessor(ObjectNode objectNode, JNode jsonNode){
        this.objectNode = objectNode;
        this.jsonNode = jsonNode;
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
        JNode value = resolveValue();
        if(value == null) return "";
        return formatString(value);
    }

    public String toPatch(){
        JNode value = resolveValue();
        if(value == null) return "";

        SugarJsonConfig config = new SugarJsonConfig().sugarStacks(sugarStacks);
        JsonTransform.sugarPatch(objectNode, value, config);
        JsonTransform.processJson(objectNode, value, true);

        if(simplifyPath) JsonTransform.simplifyPath(value);

        return formatString(value);
    }

    public String toModJson(){
        JNode value = resolveValue();
        if(value == null) return "";

        SugarJsonConfig config = new SugarJsonConfig().sugarStacks(sugarStacks);
        JsonTransform.sugarPatch(objectNode, value, config);
        JsonTransform.processJson(objectNode, value, false);

        return formatString(value);
    }

    private JNode resolveValue(){
        if(jsonNode != null) return jsonNode;
        if(patchNode != null) return JsonTransform.toJsonTree(patchNode);
        return null;
    }

    private String formatString(JNode value){
        OutputFormat fmt = format != null ? format : OutputFormat.hjson;
        if(fmt == OutputFormat.hjson){
            return value.toJson(Jformat.hjson);
        }else{
            return formatJson ? value.toJson(Jformat.formatted) : value.toJson(Jformat.plain);
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
