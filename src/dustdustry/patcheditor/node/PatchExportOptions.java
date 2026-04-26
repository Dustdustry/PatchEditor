package dustdustry.patcheditor.node;

public class PatchExportOptions{
    public enum Format{
        hjson, json
    }

    public final boolean sugarStacks;
    public final boolean simplifyPath;
    public final Format format;

    public PatchExportOptions(boolean sugarStacks, boolean simplifyPath, Format format){
        this.sugarStacks = sugarStacks;
        this.simplifyPath = simplifyPath;
        this.format = format == null ? Format.hjson : format;
    }

    public static PatchExportOptions defaults(){
        return new PatchExportOptions(true, true, Format.hjson);
    }
}