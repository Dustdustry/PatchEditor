package dustdustry.patcheditor.node;

public class PatchExportOptions{
    public enum Format{
        hjson, json
    }

    public boolean sugarStacks;
    public boolean simplifyPath;
    public boolean formatJson;
    public final Format format;

    public PatchExportOptions(boolean sugarStacks, boolean simplifyPath, boolean formatJson, Format format){
        this.sugarStacks = sugarStacks;
        this.simplifyPath = simplifyPath;
        this.formatJson = formatJson;
        this.format = format == null ? Format.hjson : format;
    }

    public static PatchExportOptions defaults(){
        return new PatchExportOptions(true, true, true, Format.hjson);
    }
}