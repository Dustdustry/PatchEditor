package MinRi2.PatchEditor.ui.dialog;

import MinRi2.PatchEditor.export.ObjectExporter.*;
import MinRi2.PatchEditor.node.*;
import arc.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.ui.dialogs.SettingsMenuDialog.*;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable.*;

import static arc.Core.settings;

public class EditorSettings extends BaseDialog{
    public EditorSettings(){
        super("@patch-editor.settings");

        fallback();

        shown(() -> {
            if(!cont.hasChildren()) setup();
        });
    }

    private void fallback(){
        if(settings.has("patch-editor.simplifyPatch")){
            boolean simplifyPatch = settings.getBool("patch-editor.simplifyPatch");
            settings.put("patch-editor.simplifyPath", simplifyPatch);
            settings.remove("patch-editor.simplifyPatch");
        }
    }

    private void setup(){
        SettingsTable table = new SettingsTable();
        Seq<Setting> settings = table.getSettings();
        cont.pane(Styles.noBarPane, table);

        table.checkPref("patch-editor.simplifyPath", true);
        table.checkPref("patch-editor.sugar.stacks", true);
        table.checkPref("patch-editor.magicExport.allowDefault", false);
        table.checkPref("patch-editor.editNotes", false);

        table.sliderPref("patch-editor.undoLimit", 20, 0, 160, 20,s -> Core.bundle.format("setting.patch-editor.undoLimit.text", s));
        settings.add(new SingleEnumSettings("patch-editor.exportType", ExportType.values(), ExportType.hjson));

        table.rebuild();
        addCloseButton();
    }

    public static ExportConfig getExportConfig(){
        ExportConfig config = new ExportConfig();
        config.allowDefault = settings.getBool("patch-editor.magicExport.allowDefault");
        return config;
    }

    public static PatchExportOptions getPatchExportOptions(){
        String exportType = settings.getString("patch-editor.exportType");
        PatchExportOptions.Format format = ExportType.hjson.is(exportType) ? PatchExportOptions.Format.hjson : PatchExportOptions.Format.json;
        return new PatchExportOptions(
        settings.getBool("patch-editor.sugar.stacks"),
        settings.getBool("patch-editor.simplifyPath"),
        format
        );
    }

    public enum ExportType{
        hjson, json;

        public boolean is(String name){
            return name().equals(name);
        }
    }

    public static class SingleEnumSettings extends Setting{
        public Enum<?>[] enums;
        public Enum<?> def;

        public SingleEnumSettings(String name, Enum<?>[] enums, Enum<?> def){
            super(name);

            this.enums = enums;
            this.def = def;

            settings.defaults(name, def.name());
        }

        @Override
        public void add(SettingsTable table){
            table.table(cont -> {
                cont.table(top -> {
                    top.left();
                    top.image(Icon.settings);
                    top.add(title).padLeft(8f);
                }).growX().row();

                cont.table(buttons -> {
                    for(Enum<?> anEnum : enums){
                        String text = Core.bundle.get(name + "." + anEnum.name(), anEnum.name());
                        buttons.button(text, Styles.clearTogglet, () -> {
                            settings.put(name, anEnum.name());
                        }).margin(4f).growX().checked(b -> anEnum.name().equals(settings.getString(name)));
                    }
                }).padTop(3f).growX();

                addDesc(cont);
            }).pad(6f).growX();

            table.row();
        }
    }
}
