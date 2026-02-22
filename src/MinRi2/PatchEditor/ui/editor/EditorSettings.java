package MinRi2.PatchEditor.ui.editor;

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

        shown(() -> {
            if(!cont.hasChildren()) setup();
        });
    }

    private void setup(){
        SettingsTable table = new SettingsTable();
        Seq<Setting> settings = table.getSettings();
        cont.pane(Styles.noBarPane, table);

        table.checkPref("patch-editor.simplifyPatch", true);
        settings.add(new SingleEnumSettings("patch-editor.exportType", ExportType.values(), ExportType.hjson));

        table.rebuild();
        addCloseButton();
    }

    public enum ExportType{
        hjson, json
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
