package dustdustry.patcheditor.node;

import dustdustry.patcheditor.node.JsonProcessor.OutputFormat;

public class PatchExportOptions{
    public boolean sugarStacks;
    public boolean simplifyPath;
    public boolean formatJson;
    public final OutputFormat format;

    public PatchExportOptions(boolean sugarStacks, boolean simplifyPath, boolean formatJson, OutputFormat format){
        this.sugarStacks = sugarStacks;
        this.simplifyPath = simplifyPath;
        this.formatJson = formatJson;
        this.format = format == null ? OutputFormat.hjson : format;
    }

    public static PatchExportOptions defaults(){
        return new PatchExportOptions(true, true, true, OutputFormat.hjson);
    }
}